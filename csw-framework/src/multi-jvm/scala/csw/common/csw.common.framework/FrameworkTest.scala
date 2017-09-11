package csw.common.framework

import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, ActorSystem}
import com.typesafe.config.ConfigFactory
import csw.common.components.{ComponentStatistics, SampleComponentState}
import csw.common.framework.internal.container.ContainerMode
import csw.common.framework.internal.supervisor.SupervisorMode
import csw.common.framework.internal.wiring.{Container, FrameworkWiring, Standalone}
import csw.common.framework.models.ContainerCommonMessage.{GetComponents, GetContainerMode}
import csw.common.framework.models.PubSub.Subscribe
import csw.common.framework.models.RunningMessage.Lifecycle
import csw.common.framework.models.SupervisorCommonMessage.{ComponentStateSubscription, GetSupervisorMode}
import csw.common.framework.models.ToComponentLifecycleMessage.GoOffline
import csw.common.framework.models._
import csw.param.states.CurrentState
import csw.services.location.commons.BlockingUtils
import csw.services.location.helpers.{LSNodeSpec, TwoMembersAndSeed}
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{ComponentId, ComponentType}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}

class FrameworkTestMultiJvmNode1 extends FrameworkTest(0)
class FrameworkTestMultiJvmNode2 extends FrameworkTest(0)
class FrameworkTestMultiJvmNode3 extends FrameworkTest(0)

// DEOPSCSW-167: Creation and Deployment of Standalone Components
// DEOPSCSW-169: Creation of Multiple Components
// DEOPSCSW-182: Control Life Cycle of Components
// DEOPSCSW-216: Locate and connect components to send AKKA commands
class FrameworkTest(ignore: Int) extends LSNodeSpec(config = new TwoMembersAndSeed) {

  import config._

  implicit val actorSystem: ActorSystem[_] = system.toTyped
  implicit val testkit: TestKitSettings    = TestKitSettings(actorSystem)

//  LoggingSystemFactory.start("framework", "1.0", "localhost", system)

  def waitForContainerToMoveIntoRunningMode(
      containerRef: ActorRef[ContainerExternalMessage],
      probe: TestProbe[ContainerMode],
      duration: Duration
  ): Boolean = {

    def getContainerMode: ContainerMode = {
      containerRef ! GetContainerMode(probe.ref)
      probe.expectMsgType[ContainerMode]
    }

    BlockingUtils.poll(getContainerMode == ContainerMode.Running, duration)
  }

  def waitForSupervisorToMoveIntoRunningMode(
      actorRef: ActorRef[SupervisorExternalMessage],
      probe: TestProbe[SupervisorMode],
      duration: Duration
  ): Boolean = {

    def getSupervisorMode: SupervisorMode = {
      actorRef ! GetSupervisorMode(probe.ref)
      probe.expectMsgType[SupervisorMode]
    }

    BlockingUtils.poll(getSupervisorMode == SupervisorMode.Running, duration)
  }

  test("should able to create multiple containers across jvm's and start component in standalone mode") {

    runOn(seed) {
      val containerModeProbe  = TestProbe[ContainerMode]
      val componentsProbe     = TestProbe[Components]
      val supervisorModeProbe = TestProbe[SupervisorMode]

      val wiring       = FrameworkWiring.make(system, locationService)
      val containerRef = Container.spawn(ConfigFactory.load("laser_container.conf"), wiring)

      containerRef ! GetContainerMode(containerModeProbe.ref)
      containerModeProbe.expectMsg(ContainerMode.Idle)

      waitForContainerToMoveIntoRunningMode(containerRef, containerModeProbe, 5.seconds) shouldBe true

      containerRef ! GetComponents(componentsProbe.ref)
      val laserContainerComponents = componentsProbe.expectMsgType[Components].components
      laserContainerComponents.size shouldBe 3

      // check that all the components within supervisor moves to Running mode
      laserContainerComponents
        .foreach { component ⇒
          component.supervisor ! GetSupervisorMode(supervisorModeProbe.ref)
          supervisorModeProbe.expectMsg(SupervisorMode.Running)
        }
      enterBarrier("running")

      // resolve and send message to container running in different jvm or on different physical machine
      val wfsContainerLocationF =
        locationService.resolve(AkkaConnection(ComponentId("WFS_Container", ComponentType.Container)), 2.seconds)
      val wfsContainerLocation = Await.result(wfsContainerLocationF, 5.seconds).get

      val efsContainerTypedRef = wfsContainerLocation.typedRef[ContainerMessage]

      efsContainerTypedRef ! GetContainerMode(containerModeProbe.ref)
      containerModeProbe.expectMsg(ContainerMode.Running)

      efsContainerTypedRef ! GetComponents(componentsProbe.ref)
      val components = componentsProbe.expectMsgType[Components].components
      components.size shouldBe 3
      enterBarrier("offline")
    }

    runOn(member1) {
      val containerModeProbe  = TestProbe[ContainerMode]
      val componentsProbe     = TestProbe[Components]
      val supervisorModeProbe = TestProbe[SupervisorMode]

      val wiring       = FrameworkWiring.make(system, locationService)
      val containerRef = Container.spawn(ConfigFactory.load("wfs_container.conf"), wiring)

      containerRef ! GetContainerMode(containerModeProbe.ref)
      containerModeProbe.expectMsg(ContainerMode.Idle)

      waitForContainerToMoveIntoRunningMode(containerRef, containerModeProbe, 5.seconds) shouldBe true

      containerRef ! GetComponents(componentsProbe.ref)
      val wfsContainerComponents = componentsProbe.expectMsgType[Components].components
      wfsContainerComponents.size shouldBe 3

      // check that all the components within supervisor moves to Running mode
      wfsContainerComponents
        .foreach { component ⇒
          component.supervisor ! GetSupervisorMode(supervisorModeProbe.ref)
          supervisorModeProbe.expectMsg(SupervisorMode.Running)
        }
      enterBarrier("running")

      // resolve and send message to component running in different jvm or on different physical machine
      val etonSupervisorF =
        locationService.resolve(AkkaConnection(ComponentId("Eton", ComponentType.HCD)), 2.seconds)
      val etonSupervisorLocation = Await.result(etonSupervisorF, 5.seconds).get

      val etonSupervisorTypedRef = etonSupervisorLocation.typedRef[SupervisorExternalMessage]
      val compStateProbe         = TestProbe[CurrentState]

      etonSupervisorTypedRef ! ComponentStateSubscription(Subscribe(compStateProbe.ref))
      etonSupervisorTypedRef ! ComponentStatistics(1)

      import SampleComponentState._
      compStateProbe.expectMsg(CurrentState(prefix, Set(choiceKey.set(domainChoice))))

      etonSupervisorTypedRef ! Lifecycle(GoOffline)
      enterBarrier("offline")
    }

    runOn(member2) {
      val supervisorModeProbe = TestProbe[SupervisorMode]
      val wiring              = FrameworkWiring.make(system, locationService)
      val supervisorRef       = Standalone.spawn(ConfigFactory.load("eaton_hcd_standalone.conf"), wiring)
      waitForSupervisorToMoveIntoRunningMode(supervisorRef, supervisorModeProbe, 5.seconds) shouldBe true
      enterBarrier("running")

      enterBarrier("offline")
      Thread.sleep(50)
      supervisorRef ! GetSupervisorMode(supervisorModeProbe.ref)
      supervisorModeProbe.expectMsg(SupervisorMode.RunningOffline)
    }

    enterBarrier("end")
  }
}