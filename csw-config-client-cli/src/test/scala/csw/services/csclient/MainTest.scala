package csw.services.csclient

import java.nio.file.{Files, Paths}

import csw.services.config.server.ServerWiring
import csw.services.csclient.commons.TestFutureExtension.RichFuture
import csw.services.csclient.commons.{ArgsUtil, TestFileUtils}
import csw.services.csclient.models.Options
import csw.services.csclient.utils.ArgsParser
import csw.services.location.commons.ClusterAwareSettings
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class MainTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val serverWiring = ServerWiring.make(ClusterAwareSettings.onPort(3552))
  private val httpService  = serverWiring.httpService
  httpService.registeredLazyBinding.await

  val ConfigCliApp = new Main(ClusterAwareSettings.joinLocal(3552))

  private val testFileUtils = new TestFileUtils(serverWiring.settings)

  val ArgsUtil = new ArgsUtil
  import ArgsUtil._

  override protected def beforeEach(): Unit =
    serverWiring.svnRepo.initSvnRepo()

  override protected def afterEach(): Unit = {
    testFileUtils.deleteServerFiles()
    Files.delete(Paths.get(outputFilePath))
  }

  override protected def afterAll(): Unit = {
    httpService.shutdown().await
    ConfigCliApp.shutdown().await
    Files.delete(Paths.get(inputFilePath))
    Files.delete(Paths.get(updatedInputFilePath))
  }

  test("should able to create a file in repo and read it from repo to local disk") {

    //  create file
    val parsedCreateArgs: Option[Options] = ArgsParser.parse(createMinimalArgs)
    ConfigCliApp.commandLineRunner(parsedCreateArgs.get).await

    //  get file and store it at location: /tmp/output.txt
    val parsedGetArgs: Option[Options] = ArgsParser.parse(getMinimalArgs)
    ConfigCliApp.commandLineRunner(parsedGetArgs.get).await

    // read locally saved output file (/tmp/output.conf) from disk and
    // match the contents with input file content
    readFile(outputFilePath) shouldEqual inputFileContents
  }

  test("should able to update, delete and check for existence of a file from repo") {

    //  create file
    val parsedCreateArgs: Option[Options] = ArgsParser.parse(createMinimalArgs)
    ConfigCliApp.commandLineRunner(parsedCreateArgs.get).await

    //  update file content
    val parsedUpdateArgs: Option[Options] = ArgsParser.parse(updateAllArgs)
    ConfigCliApp.commandLineRunner(parsedUpdateArgs.get).await

    //  get file and store it at location: /tmp/output.txt
    val parsedGetArgs: Option[Options] = ArgsParser.parse(getMinimalArgs)
    ConfigCliApp.commandLineRunner(parsedGetArgs.get).await

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual updatedInputFileContents

    //  file should exist
    //  is there any way to assert here?
    val parsedExistsArgs1: Option[Options] = ArgsParser.parse(existsArgs)
    ConfigCliApp.commandLineRunner(parsedExistsArgs1.get).await

    //  delete file
    val parsedDeleteArgs: Option[Options] = ArgsParser.parse(deleteArgs)
    ConfigCliApp.commandLineRunner(parsedDeleteArgs.get).await

    //  deleted file should not exist
    //  is there any way to assert here?
    val parsedExistsArgs: Option[Options] = ArgsParser.parse(existsArgs)
    ConfigCliApp.commandLineRunner(parsedExistsArgs.get).await
  }

  test("should able to set, reset and get the default version of file.") {

    //  create file
    val parsedCreateArgs: Option[Options] = ArgsParser.parse(createMinimalArgs)
    ConfigCliApp.commandLineRunner(parsedCreateArgs.get).await

    //  update file content
    val parsedUpdateArgs: Option[Options] = ArgsParser.parse(updateAllArgs)
    ConfigCliApp.commandLineRunner(parsedUpdateArgs.get).await

    //  set default version of file to id=1 and store it at location: /tmp/output.txt
    val parsedSetDefaultArgs: Option[Options] = ArgsParser.parse(setDefaultAllArgs)
    ConfigCliApp.commandLineRunner(parsedSetDefaultArgs.get).await

    //  get default version of file and store it at location: /tmp/output.txt
    val parsedGetDefaultArgs: Option[Options] = ArgsParser.parse(getDefaultArgs)
    ConfigCliApp.commandLineRunner(parsedGetDefaultArgs.get).await

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual inputFileContents

    //  reset default version of file and store it at location: /tmp/output.txt
    val parsedResetDefaultArgs: Option[Options] = ArgsParser.parse(setDefaultMinimalArgs)
    ConfigCliApp.commandLineRunner(parsedResetDefaultArgs.get).await

    //  get default version of file and store it at location: /tmp/output.txt
    ConfigCliApp.commandLineRunner(parsedGetDefaultArgs.get).await

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual updatedInputFileContents
  }
}