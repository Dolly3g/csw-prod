package csw.framework.internal.component

import akka.typed.testkit.StubbedActorContext
import akka.typed.testkit.scaladsl.TestProbe
import csw.framework.scaladsl.ComponentHandlers
import csw.framework.{ComponentInfos, FrameworkTestSuite}
import csw.messages.FromComponentLifecycleMessage.Running
import csw.messages.IdleMessage.Initialize
import csw.messages.{CommandResponseManagerMessage, ComponentMessage, FromComponentLifecycleMessage}
import csw.services.location.scaladsl.LocationService
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

// DEOPSCSW-165-CSW Assembly Creation
// DEOPSCSW-166-CSW HCD Creation
class ComponentBehaviorTest extends FrameworkTestSuite with MockitoSugar {

  class TestData(supervisorProbe: TestProbe[FromComponentLifecycleMessage]) {

    val sampleComponentHandler: ComponentHandlers[ComponentDomainMessage] =
      mock[ComponentHandlers[ComponentDomainMessage]]
    when(sampleComponentHandler.initialize()).thenReturn(Future.unit)
    val locationService: LocationService = mock[LocationService]
    val ctx                              = new StubbedActorContext[ComponentMessage]("test-component", 100, system)
    val componentBehavior =
      new ComponentBehavior[ComponentDomainMessage](
        ctx,
        ComponentInfos.hcdInfo,
        supervisorProbe.ref,
        sampleComponentHandler,
        TestProbe[CommandResponseManagerMessage].ref,
        locationService,
        frameworkTestMocks().loggerFactory
      )
    when(sampleComponentHandler.initialize()).thenReturn(Future.unit)
  }

  test("component should start in idle lifecycle state") {
    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val testData        = new TestData(supervisorProbe)
    import testData._

    componentBehavior.lifecycleState shouldBe ComponentLifecycleState.Idle
  }

  test("component should send itself initialize message and handle initialization") {
    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val testData        = new TestData(supervisorProbe)
    import testData._

    ctx.selfInbox.receiveMsg() shouldBe Initialize

    componentBehavior.onMessage(Initialize)

    Thread.sleep(100)

    supervisorProbe.expectMsgType[Running]
    verify(sampleComponentHandler).initialize()
    verify(sampleComponentHandler).isOnline_=(true)
  }
}
