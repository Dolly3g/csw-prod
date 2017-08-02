package csw.common.framework.scaladsl.hcd

import akka.typed.testkit.scaladsl.TestProbe
import csw.common.components.hcd.HcdDomainMsg
import csw.common.framework.models.ComponentResponseMode
import csw.common.framework.models.ComponentResponseMode.{Initialized, Running}
import csw.common.framework.models.FromComponentLifecycleMessage.InitializeFailure
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.scaladsl.FrameworkComponentTestSuite
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class HcdBehaviorTest extends FrameworkComponentTestSuite with MockitoSugar {

  test("hcd component should send initialize and running message to supervisor") {
    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMsg]]

    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)

    val supervisorProbe: TestProbe[ComponentResponseMode] = TestProbe[ComponentResponseMode]

    val hcdRef =
      Await.result(
        system.systemActorOf[Nothing](getSampleHcdFactory(sampleHcdHandler).behavior(hcdInfo, supervisorProbe.ref),
                                      "sampleHcd"),
        5.seconds
      )

    val initialized = supervisorProbe.expectMsgType[Initialized]

    verify(sampleHcdHandler).initialize()
    initialized.componentRef shouldBe hcdRef

    initialized.componentRef ! Run

    val running = supervisorProbe.expectMsgType[Running]

    verify(sampleHcdHandler).onRun()
    verify(sampleHcdHandler).isOnline_=(true)

    running.componentRef shouldBe hcdRef
  }

  test("A Hcd component should send InitializationFailure message if it fails in initialization") {
    val sampleHcdHandler = mock[HcdHandlers[HcdDomainMsg]]
    val exceptionReason  = "test Exception"
    when(sampleHcdHandler.initialize()).thenThrow(new RuntimeException(exceptionReason))

    val supervisorProbe: TestProbe[ComponentResponseMode] = TestProbe[ComponentResponseMode]

    Await.result(
      system.systemActorOf[Nothing](getSampleHcdFactory(sampleHcdHandler).behavior(hcdInfo, supervisorProbe.ref),
                                    "sampleHcd"),
      5.seconds
    )

    val initializationFailure = supervisorProbe.expectMsgType[InitializeFailure]
    initializationFailure.reason shouldBe exceptionReason
  }
}
