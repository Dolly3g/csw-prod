package csw.framework.internal.supervisor

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.scaladsl.TestProbe
import com.persist.JsonOps
import com.persist.JsonOps.JsonObject
import csw.common.FrameworkAssertions._
import csw.common.components.framework.ComponentDomainMessage
import csw.common.components.framework.SampleComponentState._
import csw.common.utils.TestAppender
import csw.exceptions.{FailureRestart, FailureStop}
import csw.framework.ComponentInfos._
import csw.framework.internal.component.ComponentBehavior
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.framework.{FrameworkTestMocks, FrameworkTestSuite}
import csw.messages.CommandMessage.Submit
import csw.messages.SupervisorCommonMessage.GetSupervisorLifecycleState
import csw.messages.ccs.commands.{CommandResponse, ControlCommand, Setup}
import csw.messages.framework.{ComponentInfo, SupervisorLifecycleState}
import csw.messages.models.PubSub.{Publish, PublisherMessage}
import csw.messages.models.{LifecycleStateChanged, PubSub}
import csw.messages.params.generics.{KeyType, Parameter}
import csw.messages.params.models.ObsId
import csw.messages.params.states.CurrentState
import csw.messages.{models, _}
import csw.services.location.scaladsl.LocationService
import csw.services.logging.internal.LoggingLevels.ERROR
import csw.services.logging.internal.LoggingSystem
import csw.services.logging.scaladsl.LoggerFactory
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.stubbing.Answer
import org.scalatest.BeforeAndAfterEach

import scala.collection.mutable
import scala.concurrent.Future

// DEOPSCSW-178: Lifecycle success/failure notification
class SupervisorLifecycleFailureTest extends FrameworkTestSuite with BeforeAndAfterEach {

  val supervisorLifecycleStateProbe: TestProbe[SupervisorLifecycleState] = TestProbe[SupervisorLifecycleState]
  var supervisorRef: ActorRef[SupervisorExternalMessage]                 = _
  var initializeAnswer: Answer[Future[Unit]]                             = _
  var submitAnswer: Answer[Future[Unit]]                                 = _
  var shutdownAnswer: Answer[Future[Unit]]                               = _
  var runAnswer: Answer[Future[Unit]]                                    = _

  // all log messages will be captured in log buffer
  private val logBuffer          = mutable.Buffer.empty[JsonObject]
  private val testAppender       = new TestAppender(x ⇒ logBuffer += JsonOps.Json(x.toString).asInstanceOf[JsonObject])
  private lazy val loggingSystem = new LoggingSystem("sup-failure", "1.0", "localhost", untypedSystem)
  loggingSystem.setAppenders(List(testAppender))

  override protected def afterEach(): Unit = logBuffer.clear()

  test("handle TLA failure with FailureStop exception in initialize with Restart message") {
    val testMocks = frameworkTestMocks()
    import testMocks._

    val componentHandlers = createComponentHandlers(testMocks)

    val failureStopExMsg = "testing FailureStop"
    // Throw a `FailureStop` on the first attempt to initialize but initialize successfully on the next attempt
    doThrow(FailureStop(failureStopExMsg)).doAnswer(initializeAnswer).when(componentHandlers).initialize()

    createSupervisorAndStartTLA(testMocks, componentHandlers)

    // Component fails to initialize with `FailureStop`. The default akka supervision strategy kills the TLA
    // and triggers the PostStop signal of TLA. The post stop signal has `onShutdown` handler of the component which is
    // mocked in the test to publish a `shutdownChoice`.
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice)))))

    // DEOPSCSW-180: Generic and Specific Log messages
    // component handlers initialize block throws FailureStop exception which we expect akka logs it
    Thread.sleep(100)
    assertThatExceptionIsLogged(
      logBuffer,
      "SampleHcd",
      failureStopExMsg,
      ERROR,
      classOf[ComponentBehavior[ComponentDomainMessage]].getName,
      FailureStop.getClass.getName,
      failureStopExMsg
    )

    supervisorRef ! GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref)

    // Supervisor is still in the Idle lifecycle state
    supervisorLifecycleStateProbe.expectMsg(SupervisorLifecycleState.Idle)

    // External entity sends restart to supervisor with the intent to restart TLA
    supervisorRef ! Restart

    // TLA initialises successfully the second time due to the defined mock behavior. The `initialize` handler of the
    // component is mocked in the test to publish a `initChoice` in the second attempt.
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))

    // TLA sends `Running` message to supervisor which changes the lifecycle state of supervisor to `Running`
    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

    // Supervisor registers itself only after successful initialization of TLA. In this test the TLA successfully
    // initializes after Restart message, after which Supervisor registers itself.
    verify(locationService).register(akkaRegistration)

    // On receiving Restart message the supervisor first unregisters itself. But in the case of initialization failures
    // it never registers itself and thus never unregisters as well on receiving Restart message.
    verify(registrationResult, never()).unregister()
  }

  test("handle TLA failure with FailureRestart exception in initialize") {
    val testMocks = frameworkTestMocks()
    import testMocks._

    val componentHandlers   = createComponentHandlers(testMocks)
    val failureRestartExMsg = "testing FailureRestart"

    // Throw a `FailureRestart` on the first attempt to initialize but initialize successfully on the next attempt
    doThrow(FailureRestart(failureRestartExMsg)).doAnswer(initializeAnswer).when(componentHandlers).initialize()
    createSupervisorAndStartTLA(testMocks, componentHandlers)

    // component fails to initialize with `FailureRestart`. The akka supervision strategy specified in SupervisorBehavior
    // restarts the TLA. The `initialize` handler of the component is mocked in the test to publish a `initChoice` in the second attempt.
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))

    // TLA sends `Running` message to supervisor which changes the lifecycle state of supervisor to `Running`
    lifecycleStateProbe.expectMsg(
      Publish(models.LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))
    )

    Thread.sleep(100)
    // DEOPSCSW-180: Generic and Specific Log messages
    // component handlers initialize block throws FailureRestart exception which we expect akka logs it
    assertThatExceptionIsLogged(
      logBuffer,
      "SampleHcd",
      failureRestartExMsg,
      ERROR,
      classOf[ComponentBehavior[ComponentDomainMessage]].getName,
      FailureRestart.getClass.getName,
      failureRestartExMsg
    )

    // Supervisor registers itself only after successful initialization of TLA. In this test the TLA successfully
    // initializes after Restart message after which Supervisor registers itself
    verify(locationService).register(akkaRegistration)
  }

  // DEOPSCSW-294 : FailureRestart exception from onDomainMsg, onSetup or onObserve component handlers results into unexpected message to supervisor
  test("handle TLA failure with FailureRestart exception in Running") {
    val testMocks = frameworkTestMocks()
    import testMocks._

    val componentHandlers   = createComponentHandlers(testMocks)
    val failureRestartExMsg = "testing FailureRestart"
    val unexpectedMessage   = "Unexpected message :[Running"

    val obsId: ObsId          = ObsId("Obs001")
    val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(22)
    val setup: Setup          = Setup(obsId, successPrefix, Set(param))

    doThrow(FailureRestart(failureRestartExMsg))
      .when(componentHandlers)
      .validateCommand(any[ControlCommand])

    createSupervisorAndStartTLA(testMocks, componentHandlers)

    // component Intializes succesfully
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))

    // TLA sends `Running` message to supervisor which changes the lifecycle state of supervisor to `Running`
    lifecycleStateProbe.expectMsg(
      Publish(models.LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))
    )

    // Supervisor sends component a submit command which will fail with FailureRestart exception on calling onSubmit Handler
    supervisorRef ! Submit(setup, TestProbe[CommandResponse].ref)

    // Component initializes again by the akka framework without termination
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))

    supervisorRef ! GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref)

    // Supervisor is still in the Running lifecycle state
    supervisorLifecycleStateProbe.expectMsg(SupervisorLifecycleState.Running)

    Thread.sleep(100)

    // Assert that the error log statement of type "Unexpected message received in Running lifecycle state" is not generated
    assertThatExceptionIsNotLogged(
      logBuffer,
      unexpectedMessage,
    )
  }

  private def createSupervisorAndStartTLA(
      testMocks: FrameworkTestMocks,
      componentHandlers: ComponentHandlers[ComponentDomainMessage]
  ): Unit = {
    import testMocks._

    val supervisorBehavior = SupervisorBehaviorFactory.make(
      Some(mock[ActorRef[ContainerIdleMessage]]),
      hcdInfo,
      locationService,
      registrationFactory,
      pubSubBehaviorFactory,
      new SampleBehaviorFactory(componentHandlers),
      new LoggerFactory(hcdInfo.name)
    )

    // it creates supervisor which in turn spawns components TLA and sends Initialize and Run message to TLA
    supervisorRef = untypedSystem.spawnAnonymous(supervisorBehavior)
  }

  private def createComponentHandlers(testMocks: FrameworkTestMocks) = {
    import testMocks._

    createAnswers(compStateProbe)

    val componentHandlers = mock[ComponentHandlers[ComponentDomainMessage]]
    when(componentHandlers.initialize()).thenAnswer(initializeAnswer)
    when(componentHandlers.onShutdown()).thenAnswer(shutdownAnswer)
    componentHandlers
  }

  private def createAnswers(compStateProbe: TestProbe[PubSub[CurrentState]]): Unit = {
    initializeAnswer = (_) ⇒ Future.successful(compStateProbe.ref ! Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    shutdownAnswer = (_) ⇒
      Future.successful(compStateProbe.ref ! Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice)))))
  }
}

class SampleBehaviorFactory(componentHandlers: ComponentHandlers[ComponentDomainMessage])
    extends ComponentBehaviorFactory[ComponentDomainMessage] {
  override protected[framework] def handlers(
      ctx: ActorContext[ComponentMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: ActorRef[CommandResponseManagerMessage],
      pubSubRef: ActorRef[PublisherMessage[CurrentState]],
      locationService: LocationService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers[ComponentDomainMessage] = componentHandlers
}
