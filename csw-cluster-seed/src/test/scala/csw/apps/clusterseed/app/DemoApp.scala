package csw.apps.clusterseed.app

import akka.typed.scaladsl.adapter._
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, ActorSystem}
import com.typesafe.config.ConfigFactory
import csw.apps.clusterseed.admin.internal.AdminWiring
import csw.apps.clusterseed.components.StartLogging
import csw.common.FrameworkAssertions.assertThatContainerIsRunning
import csw.framework.internal.wiring.{Container, FrameworkWiring}
import csw.messages.ContainerCommonMessage.GetComponents
import csw.messages.ContainerMessage
import csw.messages.framework.ContainerLifecycleState
import csw.messages.models.{Component, Components}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.logging.scaladsl.{LoggerFactory, LoggingSystemFactory}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object AppLogger extends LoggerFactory("app")

/*
  This app is for testing runtime changes of component log level

  DemoApp does four things :
    1. Start seed node on port 3552
    2. Start AdminHttpServer on port 7878
    3. Creates a Laser Container with 3 components within it
    4. Laser Assembly start logging messages at all levels in infinite loop

  How to test :
    1. Start the app
    2. Import postman collection present under tools/postman which has two routes inside log admin folder (1. get log metadata 2. set log level)
    3. get log metadata :=> this will give current configuration for specified component
    4. set log level :=> update component name and value and verify on the console that logs are printed as per the updated log level.

  Important :
    Make sure you stop the app once you finish testing as it will not terminate automatically.

 */

object DemoApp extends App {

  val seedSettings             = ClusterAwareSettings.onPort(3552)
  val adminWiring: AdminWiring = AdminWiring.make(seedSettings, None)

  val frameworkSystem = ClusterAwareSettings.joinLocal(3552).system
  val frameworkWiring = FrameworkWiring.make(frameworkSystem)

  implicit val typedSystem: ActorSystem[Nothing] = frameworkWiring.actorSystem.toTyped
  implicit val testKitSettings: TestKitSettings  = TestKitSettings(typedSystem)

  private def startSeed() = {
    LoggingSystemFactory.start("logging", "version", ClusterAwareSettings.hostname, adminWiring.actorSystem)
    adminWiring.locationService
    Await.result(adminWiring.adminHttpService.registeredLazyBinding, 5.seconds)
  }

  private def spawnContainer(): ActorRef[ContainerMessage] = {

    val config       = ConfigFactory.load("laser_container.conf")
    val containerRef = Await.result(Container.spawn(config, frameworkWiring), 5.seconds)

    val containerStateProbe = TestProbe[ContainerLifecycleState]
    assertThatContainerIsRunning(containerRef, containerStateProbe, 5.seconds)
    containerRef
  }

  startSeed()

  private val containerRef: ActorRef[ContainerMessage] = spawnContainer()

  val probe = TestProbe[Components]
  containerRef ! GetComponents(probe.ref)
  val components = probe.expectMsgType[Components].components

  private val laserComponent: Component = components.find(x ⇒ x.info.name.equals("Laser")).get

  while (true) {
    println("------------------------------------")
    laserComponent.supervisor ! StartLogging()
    println("------------------------------------")
    Thread.sleep(1000)
  }
}
