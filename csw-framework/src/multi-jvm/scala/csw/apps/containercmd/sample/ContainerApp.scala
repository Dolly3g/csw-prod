package csw.apps.containercmd.sample

import akka.actor.ActorSystem
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import com.typesafe.config.{Config, ConfigFactory}
import csw.common.framework.internal.container.ContainerMode
import csw.common.framework.internal.wiring.{Container, FrameworkWiring}
import csw.common.framework.models.ContainerCommonMessage.GetContainerMode
import csw.common.framework.models.{ContainerMessage, Restart}
import csw.services.location.commons.ClusterSettings
import csw.services.logging.scaladsl.LoggingSystemFactory

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

object ContainerApp extends App {
  private val clusterSettings: ClusterSettings = ClusterSettings().withManagementPort(5555)
  private val system: ActorSystem              = clusterSettings.system
  implicit val actorSystem                     = system.toTyped
  implicit val testkit                         = TestKitSettings(actorSystem)
  private val wiring                           = FrameworkWiring.make(system)
  private val config: Config                   = ConfigFactory.load("laser_container.conf")
  LoggingSystemFactory.start("framework", "1.0", "localhost", system)
  private val ref: ActorRef[ContainerMessage] = Await.result(Container.spawn(config, wiring), 5.seconds)

  Thread.sleep(2000)

  ref ! Restart

  Thread.sleep(2000)

  private val containerModeProbe: TestProbe[ContainerMode] = TestProbe[ContainerMode]
  ref ! GetContainerMode(containerModeProbe.ref)

  containerModeProbe.expectMsg(ContainerMode.Running)
}