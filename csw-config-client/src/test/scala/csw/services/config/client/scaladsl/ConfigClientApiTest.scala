package csw.services.config.client.scaladsl

import java.nio.file.Paths

import csw.services.config.api.models.ConfigData
import csw.services.config.api.scaladsl.{ConfigClientService, ConfigService}
import csw.services.config.server.ServerWiring
import csw.services.config.server.commons.TestFileUtils
import csw.services.config.server.commons.TestFutureExtension.RichFuture
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationServiceFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

// DEOPSCSW-138: Split Config API into Admin API and Client API
// DEOPSCSW-80: HTTP based access for configuration file
class ConfigClientApiTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  private val clientLocationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(3552))

  private val serverWiring = ServerWiring.make(ClusterAwareSettings.joinLocal(3552))
  private val httpService  = serverWiring.httpService

  private val testFileUtils = new TestFileUtils(serverWiring.settings)

  import serverWiring.actorRuntime._

  //Why 2 instances of ConfigService? adminAPI is used to set configurations; clientAPI is used for validation/testing
  val configClientService: ConfigClientService = ConfigClientFactory.clientApi(actorSystem, clientLocationService)
  val configAdminService: ConfigService        = ConfigClientFactory.adminApi(actorSystem, clientLocationService)

  override protected def beforeEach(): Unit =
    serverWiring.svnRepo.initSvnRepo()

  override protected def afterEach(): Unit =
    testFileUtils.deleteServerFiles()

  override protected def beforeAll(): Unit =
    httpService.registeredLazyBinding.await

  override protected def afterAll(): Unit = {
    httpService.shutdown().await
    clientLocationService.shutdown().await
  }

  val configValue1: String =
    """
      |axisName1 = tromboneAxis1
      |axisName2 = tromboneAxis2
      |axisName3 = tromboneAxis3
      |""".stripMargin

  val configValue2: String =
    """
      |axisName11 = tromboneAxis11
      |axisName22 = tromboneAxis22
      |axisName33 = tromboneAxis33
      |""".stripMargin

  val configValue3: String =
    """
      |axisName111 = tromboneAxis111
      |axisName222 = tromboneAxis222
      |axisName333 = tromboneAxis333
      |""".stripMargin

  test("should able to get, set and reset the active version of config file") {
    // create file
    val file = Paths.get("/tmt/test/setactive/getactive/resetactive/active.conf")
    configAdminService.create(file, ConfigData.fromString(configValue1), annex = false, "First commit").await
    configClientService.exists(file).await shouldBe true

    // update file twice
    val configId = configAdminService.update(file, ConfigData.fromString(configValue2), "second commit").await
    configAdminService.update(file, ConfigData.fromString(configValue3), "third commit").await

    // check that get file without ID should return latest file
    configAdminService.getLatest(file).await.get.toStringF.await shouldBe configValue3
    // check that getActive call before any setActive call should return the file with id with which it was created
    configClientService.getActive(file).await.get.toStringF.await shouldBe configValue1
    // set active version of file to id=2
    configAdminService.setActiveVersion(file, configId, "Setting active version for the first time").await
    // check that getActive file without ID returns file with id=2
    configClientService.getActive(file).await.get.toStringF.await shouldBe configValue2
  }
}
