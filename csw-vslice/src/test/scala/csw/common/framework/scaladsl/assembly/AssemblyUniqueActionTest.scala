package csw.common.framework.scaladsl.assembly

import akka.typed.testkit.scaladsl.TestProbe
import csw.common.components.assembly.{AssemblyDomainMsg, OperationsMode}
import csw.common.framework.models.ComponentResponseMode
import csw.common.framework.models.ComponentResponseMode.{Initialized, Running}
import csw.common.framework.models.InitialMsg.Run
import csw.common.framework.scaladsl.FrameworkComponentTestSuite
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class AssemblyUniqueActionTest extends FrameworkComponentTestSuite with MockitoSugar {

  test("assembly component should be able to handle Domain specific messages") {
    val sampleAssemblyHandler = mock[AssemblyHandlers[AssemblyDomainMsg]]

    when(sampleAssemblyHandler.initialize()).thenReturn(Future.unit)

    val supervisorProbe: TestProbe[ComponentResponseMode] = TestProbe[ComponentResponseMode]

    Await.result(
      system.systemActorOf[Nothing](
        getSampleAssemblyFactory(sampleAssemblyHandler).behavior(assemblyInfo, supervisorProbe.ref),
        "sampleAssembly"
      ),
      5.seconds
    )

    val initialized = supervisorProbe.expectMsgType[Initialized]
    initialized.componentRef ! Run

    val running = supervisorProbe.expectMsgType[Running]
    running.componentRef ! OperationsMode

    Thread.sleep(1000)
    verify(sampleAssemblyHandler).onDomainMsg(OperationsMode)
  }
}
