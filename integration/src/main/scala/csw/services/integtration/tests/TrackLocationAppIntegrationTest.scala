package csw.services.integtration.tests

import java.io.{BufferedWriter, FileWriter}
import java.net.URI

import akka.actor.ActorSystem
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.internal.{Networks, Settings}
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models._
import csw.services.location.scaladsl.{ActorRuntime, LocationService}
import csw.services.tracklocation.TrackLocationApp
import org.scalatest._

import scala.concurrent.Future

class TrackLocationAppIntegrationTest(locationService: LocationService)
  extends FunSuite
    with Matchers
    with BeforeAndAfter
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

//  private val locationService = LocationServiceFactory.make()
  private val actorSystem = ActorSystem("test")
  import actorSystem.dispatcher

  val trackLocationApp = new TrackLocationApp(new ActorRuntime(Settings().withPort(2556)))

  override protected def afterAll(): Unit = {
    actorSystem.terminate()
    trackLocationApp.shutdown().await
//    locationService.shutdown().await
  }

  test("launch the trackLocationApp") {
    val name = "test1"
    val port = 9999

    Future {
      trackLocationApp.start(
        Array(
          "--name", name,
          "--command",
          "sleep 5",
          "--port", port.toString,
          "--no-exit"
        )
      )
    }

    Thread.sleep(2000)

    val connection = TcpConnection(ComponentId(name, ComponentType.Service))
    val tcpLocation = locationService.resolve(connection).await.get
    tcpLocation shouldBe TcpLocation(connection, new URI(s"tcp://${new Networks().hostname()}:$port"))

    //Below sleep should allow TrackLocation->LocationService->UnregisterAll to propogate test's locationService
    Thread.sleep(6000)

    val locations: Seq[Location] = locationService.list.await
    locations.contains(tcpLocation) shouldBe false
  }

  test("Test with config file") {
    val name = "test2"
    val port = 8888

    val tempFile = java.io.File.createTempFile("trackLocationApp-test2", ".conf")
    val bw = new BufferedWriter(new FileWriter(tempFile))
    bw.write(s"""test2 {
      port = 8888
      command = sleep 5
    }""")
    bw.close()

    val configFile: String = tempFile.getPath
    println(configFile)

    Future {
      trackLocationApp.start(
        Array(
          "--name",
          name,
          "--no-exit",
          configFile)
      )
    }

    Thread.sleep(2000)

    val connection = TcpConnection(ComponentId(name, ComponentType.Service))
    val tcpLocation = locationService.resolve(connection).await.get
    tcpLocation shouldBe TcpLocation(connection, new URI(s"tcp://${new Networks().hostname()}:$port"))

    //Below sleep should allow TrackLocation->LocationService->UnregisterAll to propogate test's locationService
    Thread.sleep(6000)

    val locations: Seq[Location] = locationService.list.await
    locations.contains(tcpLocation) shouldBe false

    tempFile.delete()
  }
}
