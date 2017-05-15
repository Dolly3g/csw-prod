package csw.services.config.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.ConfigFactory
import csw.services.config.server.commons.TestFileUtils
import csw.services.config.server.commons.TestFutureExtension.RichFuture
import csw.services.location.commons.ClusterSettings
import csw.services.location.models.Connection.HttpConnection
import csw.services.location.models.{ComponentId, ComponentType}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}
import org.tmatesoft.svn.core.SVNException
import org.tmatesoft.svn.core.io.SVNRepositoryFactory

import scala.concurrent.duration._

// DEOPSCSW-130: Command line App for HTTP server
class MainTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {
  implicit val actorSystem: ActorSystem = ActorSystem("config-server")
  implicit val mat: Materializer        = ActorMaterializer()

  private val clusterPort = 3789
  private val locationService: LocationService =
    LocationServiceFactory.withSettings(ClusterSettings().onPort(clusterPort))

  private val clusterSettings = ClusterSettings().joinLocal(clusterPort)

  private val settings      = new Settings(ConfigFactory.load())
  private val testFileUtils = new TestFileUtils(settings)

  override protected def afterEach(): Unit =
    testFileUtils.deleteServerFiles()

  override protected def afterAll(): Unit = {
    actorSystem.terminate().await
    locationService.shutdown().await
  }

  test("should not start HTTP server if repo does not exist") {
    val actualException = intercept[Exception] {
      new Main(clusterSettings).start(Array.empty).get
    }

    actualException.getCause shouldBe a[SVNException]

    val configConnection = HttpConnection(ComponentId("ConfigServiceServer", ComponentType.Service))
    locationService.find(configConnection).await shouldBe None
  }

  test("should init svn repo and register with location service if --initRepo option is provided") {
    SVNRepositoryFactory.createLocalRepository(settings.repositoryFile, false, false)
    val httpService = new Main(clusterSettings).start(Array("--initRepo")).get

    try {
      val configConnection      = HttpConnection(ComponentId("ConfigServiceServer", ComponentType.Service))
      val configServiceLocation = locationService.resolve(configConnection, 5.seconds).await.get
      configServiceLocation.connection shouldBe configConnection

      val uri = Uri(configServiceLocation.uri.toString).withPath(Path / "list")

      val request  = HttpRequest(uri = uri)
      val response = Http().singleRequest(request).await
      response.status shouldBe StatusCodes.OK
      response.discardEntityBytes()
    } finally {
      httpService.shutdown().await
    }
  }

  test("should not initialize svn repo if --initRepo option is not provided and should use existing repo if available") {
    SVNRepositoryFactory.createLocalRepository(settings.repositoryFile, false, false)
    // temporary start a server to create a repo and then shutdown the server
    val tmpHttpService = new Main(clusterSettings).start(Array("--initRepo")).get
    tmpHttpService.shutdown().await

    val httpService = new Main(clusterSettings).start(Array.empty).get

    try {
      val configConnection      = HttpConnection(ComponentId("ConfigServiceServer", ComponentType.Service))
      val configServiceLocation = locationService.resolve(configConnection, 5.seconds).await.get

      configServiceLocation.connection shouldBe configConnection
      val uri = Uri(configServiceLocation.uri.toString).withPath(Path / "list")

      val request  = HttpRequest(uri = uri)
      val response = Http().singleRequest(request).await
      response.status shouldBe StatusCodes.OK
    } finally {
      httpService.shutdown().await
    }
  }
}
