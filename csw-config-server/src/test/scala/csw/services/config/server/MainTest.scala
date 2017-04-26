package csw.services.config.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.ConfigFactory
import csw.services.config.server.commons.TestFileUtils
import csw.services.config.server.commons.TestFutureExtension.RichFuture
import csw.services.config.server.http.HttpService
import csw.services.location.commons.ClusterSettings
import csw.services.location.models.Connection.HttpConnection
import csw.services.location.models.{ComponentId, ComponentType}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class MainTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {
  implicit val actorSystem: ActorSystem = ActorSystem("config-server")
  implicit val mat: Materializer        = ActorMaterializer()

  private val clusterPort = 3789
  private val locationService: LocationService =
    LocationServiceFactory.withSettings(ClusterSettings().onPort(clusterPort))

  private val clusterSettings = ClusterSettings().joinLocal(clusterPort)

  private val testFileUtils = new TestFileUtils(new Settings(ConfigFactory.load()))

  override protected def afterEach(): Unit =
    testFileUtils.deleteServerFiles()

  override protected def afterAll(): Unit = {
    actorSystem.terminate().await
    locationService.shutdown().await
  }

  test("should be able to register with location service on start up") {
    val httpService: HttpService = new Main(clusterSettings, Array()).maybeHttpService.get
    val configConnection         = HttpConnection(ComponentId("ConfigServiceServer", ComponentType.Service))
    locationService.find(configConnection).await.get.connection shouldBe configConnection
    httpService.shutdown().await
    locationService.find(configConnection).await shouldBe None
  }

  test("should init svn repo if --initRepo option is provided") {
    val httpService = new Main(clusterSettings, Array("--initRepo")).maybeHttpService.get

    val configConnection      = HttpConnection(ComponentId("ConfigServiceServer", ComponentType.Service))
    val configServiceLocation = locationService.find(configConnection).await.get

    val uri = Uri(configServiceLocation.uri.toString).withPath(Path / "list")

    val request  = HttpRequest(uri = uri)
    val response = Http().singleRequest(request).await
    response.status shouldBe StatusCodes.OK
    response.discardEntityBytes()

    httpService.shutdown().await
  }

  test("should not initialize svn repo if --initRepo option is not provided") {
    val httpService = new Main(clusterSettings, Array[String]()).maybeHttpService.get

    val configConnection      = HttpConnection(ComponentId("ConfigServiceServer", ComponentType.Service))
    val configServiceLocation = locationService.find(configConnection).await.get

    val uri = Uri(configServiceLocation.uri.toString).withPath(Path / "list")

    val request  = HttpRequest(uri = uri)
    val response = Http().singleRequest(request).await
    response.status shouldBe StatusCodes.InternalServerError

    httpService.shutdown().await
  }
}