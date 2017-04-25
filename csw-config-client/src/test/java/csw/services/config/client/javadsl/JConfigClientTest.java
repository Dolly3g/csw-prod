package csw.services.config.client.javadsl;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import csw.services.config.api.exceptions.FileAlreadyExists;
import csw.services.config.api.exceptions.FileNotFound;
import csw.services.config.api.javadsl.IConfigService;
import csw.services.config.api.models.ConfigData;
import csw.services.config.api.models.ConfigFileHistory;
import csw.services.config.api.models.ConfigFileInfo;
import csw.services.config.api.models.ConfigId;
import csw.services.config.client.internal.ActorRuntime;
import csw.services.config.server.ServerWiring;
import csw.services.config.server.commons.TestFileUtils;
import csw.services.config.server.http.HttpService;
import csw.services.location.commons.ClusterAwareSettings;
import csw.services.location.javadsl.ILocationService;
import csw.services.location.javadsl.JLocationServiceFactory;
import org.junit.*;
import org.junit.rules.ExpectedException;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.isA;

public class JConfigClientTest {
    private static ActorRuntime actorRuntime = new ActorRuntime(ActorSystem.create());
    private static ILocationService clientLocationService = JLocationServiceFactory.withSettings(ClusterAwareSettings.onPort(3552));
    private static IConfigService configService = JConfigClientFactory.make(actorRuntime.actorSystem(), clientLocationService);

    private static ServerWiring serverWiring = ServerWiring.make(ClusterAwareSettings.joinLocal(3552, new scala.collection.mutable.ArrayBuffer()));
    private static HttpService httpService = serverWiring.httpService();
    private TestFileUtils testFileUtils = new TestFileUtils(serverWiring.settings());

    private Materializer mat = actorRuntime.mat();

    private String configValue1 = "axisName1 = tromboneAxis\naxisName2 = tromboneAxis2\naxisName3 = tromboneAxis3";
    private String configValue2 = "axisName11 = tromboneAxis\naxisName22 = tromboneAxis2\naxisName3 = tromboneAxis33";
    private String configValue3 = "axisName111 = tromboneAxis\naxisName222 = tromboneAxis2\naxisName3 = tromboneAxis333";
    private String configValue4 = "axisName1111 = tromboneAxis\naxisName2222 = tromboneAxis2\naxisName3 = tromboneAxis3333";
    private String configValue5 = "axisName11111 = tromboneAxis\naxisName22222 = tromboneAxis2\naxisName3 = tromboneAxis3333";

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void beforeAll() throws Exception {
        Await.result(httpService.registeredLazyBinding(), Duration.create(20, "seconds"));
    }

    @Before
    public void initSvnRepo() {
        serverWiring.svnRepo().initSvnRepo();
    }

    @After
    public void deleteServerFiles() {
        testFileUtils.deleteServerFiles();
    }

    @AfterClass
    public static void afterAll() throws Exception {
        Await.result(httpService.shutdown(), Duration.create(20, "seconds"));
        clientLocationService.shutdown().get();
        Await.result(actorRuntime.actorSystem().terminate(), Duration.create(20, "seconds"));
    }

    // DEOPSCSW-42: Storing text based component configuration
    @Test
    public void testCreateAndRetrieveFile() throws ExecutionException, InterruptedException {
        Path path = Paths.get("/tmt/trombone/assembly/conf/normalfiles/test/test.conf");
        configService.create(path, ConfigData.fromString(configValue1), false, "commit test file").get();
        Optional<ConfigData> configData = configService.get(path).get();
        Assert.assertEquals(configData.get().toJStringF(mat).get(), configValue1);
    }

    // DEOPSCSW-42: Storing text based component configuration
    @Test
    public void testFileAlreadyExistsExceptionOnCreate() throws ExecutionException, InterruptedException {
        Path path = Paths.get("/tmt/trombone/assembly/conf/normalfiles/test/test.conf");
        configService.create(path, ConfigData.fromString(configValue1), false, "commit test file").get();
        exception.expectCause(isA(FileAlreadyExists.class));
        configService.create(path, ConfigData.fromString(configValue1), false, "commit test file").get();
    }

    @Test
    public void testCreateOversizeFile() throws ExecutionException, InterruptedException {
        Path path = Paths.get("SomeOversizeFile.txt");
        configService.create(path, ConfigData.fromString(configValue1), true).get();
        Optional<ConfigData> configData = configService.get(path).get();
        Assert.assertEquals(configData.get().toJStringF(mat).get(), configValue1);
    }

    @Test
    public void testUpdateExistingFile() throws ExecutionException, InterruptedException {
        Path path = Paths.get("/assembly.conf");
        configService.create(path, ConfigData.fromString(configValue1), false, "commit assembly conf").get();
        Optional<ConfigData> configData = configService.get(path).get();
        Assert.assertEquals(configData.get().toJStringF(mat).get(), configValue1);

        configService.update(path, ConfigData.fromString(configValue2), "commit updated assembly conf").get();
        Optional<ConfigData> configDataUpdated = configService.get(path).get();
        Assert.assertEquals(configDataUpdated.get().toJStringF(mat).get(), configValue2);
    }

    @Test
    public void testUpdateReturnsFileNotFoundExceptionOnAbsenceOfFile() throws ExecutionException, InterruptedException {
        Path path = Paths.get("/tmt/trombone/assembly.conf");
        exception.expectCause(isA(FileNotFound.class));
        configService.update(path, ConfigData.fromString(configValue1), "commit assembly conf").get();
    }

    @Test
    public void testGetReturnsNoneIfFileNotExists() throws ExecutionException, InterruptedException {
        Path path = Paths.get("/tmt/text/file/not/exist/app.conf");
        Assert.assertEquals(configService.get(path).get(), Optional.empty());
    }

    // DEOPSCSW-46: Unique identifier for configuration file version
    @Test
    public void  testEachRevisionHasUniqueId() throws ExecutionException, InterruptedException {
        Path tromboneHcdConf       = Paths.get("trombone/test/hcd/akka/hcd.conf");
        Path tromboneAssemblyConf  = Paths.get("trombone/test/assembly/akka/assembly.conf");
        Path tromboneContainerConf = Paths.get("trombone/test/container/akka/container.conf");
        Path redisConf             = Paths.get("redis/test/text/redis.conf");

        ConfigId configId1 = configService.create(tromboneHcdConf, ConfigData.fromString(configValue1)).get();
        ConfigId configId2 = configService.create(tromboneAssemblyConf, ConfigData.fromString(configValue2)).get();
        ConfigId configId3 = configService.create(redisConf, ConfigData.fromString(configValue3)).get();
        ConfigId configId4 = configService.create(tromboneContainerConf, ConfigData.fromString(configValue4)).get();
        ConfigId configId5 = configService.update(tromboneHcdConf, ConfigData.fromString(configValue5)).get();
        ConfigId configId6 = configService.update(tromboneAssemblyConf, ConfigData.fromString(configValue2)).get();

        ConfigData configData1 = configService.get(tromboneHcdConf, Optional.of(configId1)).get().get();
        Assert.assertEquals(configData1.toJStringF(mat).get(), configValue1);

        ConfigData configData2 = configService.get(tromboneAssemblyConf, Optional.of(configId2)).get().get();
        Assert.assertEquals(configData2.toJStringF(mat).get(), configValue2);

        ConfigData configData3 = configService.get(redisConf, Optional.of(configId3)).get().get();
        Assert.assertEquals(configData3.toJStringF(mat).get(), configValue3);

        ConfigData configData4 = configService.get(tromboneContainerConf, Optional.of(configId4)).get().get();
        Assert.assertEquals(configData4.toJStringF(mat).get(), configValue4);

        ConfigData configData5 = configService.get(tromboneHcdConf, Optional.of(configId5)).get().get();
        Assert.assertEquals(configData5.toJStringF(mat).get(), configValue5);

        ConfigData configData6 = configService.get(tromboneAssemblyConf, Optional.of(configId6)).get().get();
        Assert.assertEquals(configData6.toJStringF(mat).get(), configValue2);
    }

    @Test
    public void testSpecificVersionRetrieval() throws ExecutionException, InterruptedException {
        Path path = Paths.get("/a/b/csw.conf");
        configService.create(path, ConfigData.fromString(configValue1), false, "commit csw conf path").get();
        Assert.assertEquals(configService.get(path).get().get().toJStringF(mat).get(), configValue1);

        ConfigId configId = configService.update(path, ConfigData.fromString(configValue2), "commit updated conf path").get();

        configService.update(path, ConfigData.fromString(configValue3), "updated config to assembly").get();
        Assert.assertEquals(configService.get(path).get().get().toJStringF(mat).get(), configValue3);

        Assert.assertEquals(configService.get(path, Optional.of(configId)).get().get().toJStringF(mat).get(), configValue2);
    }

    @Test
    public void testRetrieveVersionBasedOnDate() throws ExecutionException, InterruptedException {
        Path path = Paths.get("/test.conf");
        configService.create(path, ConfigData.fromString(configValue1), false, "commit initial configuration").get();
        Assert.assertEquals(configService.get(path).get().get().toJStringF(mat).get(), configValue1);

        configService.update(path, ConfigData.fromString(configValue2), "updated config to assembly").get();
        Instant instant = Instant.now();
        configService.update(path, ConfigData.fromString(configValue3), "updated config to assembly").get();

        Assert.assertEquals(configService.get(path).get().get().toJStringF(mat).get(), configValue3);
        Assert.assertEquals(configService.get(path, instant).get().get().toJStringF(mat).get(), configValue2);
    }

    @Test
    public void testHistoryOfAFile() throws ExecutionException, InterruptedException {
        Path path = Paths.get("/test.conf");
        ConfigId configIdCreate = configService.create(path, ConfigData.fromString(configValue1), false, "commit initial configuration").get();
        Assert.assertEquals(configService.get(path).get().get().toJStringF(mat).get(), configValue1);

        ConfigId configIdUpdate1 = configService.update(path, ConfigData.fromString(configValue2), "updated config to assembly").get();
        ConfigId configIdUpdate2 = configService.update(path, ConfigData.fromString(configValue3), "updated config to assembly").get();

        Assert.assertEquals(configService.history(path).get().size(), 3);
        Assert.assertEquals(configService.history(path).get().stream().map(ConfigFileHistory::id).collect(Collectors.toList()),
                new ArrayList<>(Arrays.asList(configIdUpdate2, configIdUpdate1, configIdCreate)));

        Assert.assertEquals(configService.history(path, 2).get().size(), 2);
        Assert.assertEquals(configService.history(path, 2).get().stream().map(ConfigFileHistory::id).collect(Collectors.toList()),
                new ArrayList<>(Arrays.asList(configIdUpdate2, configIdUpdate1)));
    }

    @Test
    public void testListAllFiles() throws ExecutionException, InterruptedException {
        Path tromboneConfig = Paths.get("trombone.conf");
        Path assemblyConfig = Paths.get("a/b/assembly/assembly.conf");

        String tromboneConfigComment = "hello trombone";
        String assemblyConfigComment = "hello assembly";

        ConfigId tromboneConfigId = configService.create(tromboneConfig, ConfigData.fromString("axisName = tromboneAxis"), false, tromboneConfigComment).get();
        ConfigId assemblyConfigId = configService.create(assemblyConfig, ConfigData.fromString("assemblyHCDCount = 3"), false, assemblyConfigComment).get();

        ConfigFileInfo tromboneConfigInfo = new ConfigFileInfo(tromboneConfig, tromboneConfigId, tromboneConfigComment);
        ConfigFileInfo assemblyConfigInfo = new ConfigFileInfo(assemblyConfig, assemblyConfigId, assemblyConfigComment);

        Assert.assertEquals(configService.list().get(), new ArrayList<>(Arrays.asList(assemblyConfigInfo, tromboneConfigInfo)));
    }

    @Test
    public void testExists() throws ExecutionException, InterruptedException {
        Path path = Paths.get("/test.conf");
        Assert.assertFalse(configService.exists(path).get());

        Path path1 = Paths.get("a/test.csw.conf");
        configService.create(path1, ConfigData.fromString(configValue1), false, "commit config file").get();

        Assert.assertTrue(configService.exists(path1).get());
    }

    @Test
    public void testDelete() throws ExecutionException, InterruptedException {
        Path path = Paths.get("tromboneHCD.conf");
        configService.create(path, ConfigData.fromString(configValue1), false, "commit trombone config file").get();

        Assert.assertEquals(configService.get(path).get().get().toJStringF(mat).get(), configValue1);

        configService.delete(path).get();
        Assert.assertEquals(configService.get(path).get(), Optional.empty());
    }

    @Test
    public void testGetAndSetDefaultConfigFile() throws ExecutionException, InterruptedException {
        Path path = Paths.get("/tmt/text-files/trombone_hcd/application.conf");
        configService.create(path, ConfigData.fromString(configValue1), false, "hello world").get();
        Assert.assertEquals(configService.get(path).get().get().toJStringF(mat).get(), configValue1);

        ConfigId configIdUpdate1 = configService.update(path, ConfigData.fromString(configValue2), "Updated config to assembly").get();
        configService.update(path, ConfigData.fromString(configValue3), "Updated config to assembly").get();
        Assert.assertEquals(configService.getDefault(path).get().get().toJStringF(mat).get(), configValue3);

        configService.setDefault(path, Optional.of(configIdUpdate1)).get();
        Assert.assertEquals(configService.getDefault(path).get().get().toJStringF(mat).get(), configValue2);

        configService.setDefault(path, Optional.empty()).get();
        Assert.assertEquals(configService.getDefault(path).get().get().toJStringF(mat).get(), configValue3);
    }

    @Test
    public void testGetDefaultReturnsNoneIfFileNotExist() throws ExecutionException, InterruptedException {
        Path path = Paths.get("/tmt/text-files/trombone_hcd/application.conf");
        Assert.assertEquals(configService.getDefault(path).get(), Optional.empty());
    }

    @Test
    public void testListOversizeFiles() throws ExecutionException, InterruptedException {
        Path tromboneConfig = Paths.get("trombone.conf");
        Path assemblyConfig = Paths.get("a/b/assembly/assembly.conf");

        String tromboneConfigComment = "test{Oversize file no1}";
        String assemblyConfigComment = "test{Oversize file no2}";

        ConfigId tromboneConfigId = configService.create(tromboneConfig, ConfigData.fromString("axisName = tromboneAxis"),
                                                        true,
                                                         tromboneConfigComment).get();
        ConfigId assemblyConfigId = configService.create(assemblyConfig, ConfigData.fromString("assemblyHCDCount = 3"),
                                                        true,
                                                         assemblyConfigComment).get();

        ConfigFileInfo tromboneConfigInfo = new ConfigFileInfo(
                Paths.get(tromboneConfig.toString() + serverWiring.settings().sha1$minussuffix()),
                          tromboneConfigId, tromboneConfigComment);
        ConfigFileInfo assemblyConfigInfo = new ConfigFileInfo(
                Paths.get(assemblyConfig.toString() + serverWiring.settings().sha1$minussuffix()),
                          assemblyConfigId, assemblyConfigComment);

        Assert.assertEquals(configService.list().get(), new ArrayList<>(Arrays.asList(assemblyConfigInfo, tromboneConfigInfo)));
    }

    @Test
    public void testOversizeFileExists() throws ExecutionException, InterruptedException {
        Path path = Paths.get("/test.conf");
        Assert.assertFalse(configService.exists(path).get());

        Path newPath = Paths.get("a/test.csw.conf");
        configService.create(newPath, ConfigData.fromString(configValue3), true, "create oversize file").get();

        Assert.assertTrue(configService.exists(newPath).get());
    }

    @Test
    public void testUpdateAndHistoryOfOversizedFiles() throws ExecutionException, InterruptedException {
        Path path = Paths.get("/tmt/binary-files/trombone_hcd/app.bin");
        ConfigId configIdCreate = configService.create(path, ConfigData.fromString(configValue1), true, "commit initial configuration").get();
        Assert.assertEquals(configService.get(path).get().get().toJStringF(mat).get(), configValue1);

        ConfigId configIdUpdate1 = configService.update(path, ConfigData.fromString(configValue2), "updated config to assembly").get();
        ConfigId configIdUpdate2 = configService.update(path, ConfigData.fromString(configValue3), "updated config to assembly").get();

        Assert.assertEquals(configService.history(path).get().size(), 3);
        Assert.assertEquals(configService.history(path).get().stream().map(ConfigFileHistory::id).collect(Collectors.toList()),
                new ArrayList<>(Arrays.asList(configIdUpdate2, configIdUpdate1, configIdCreate)));

        Assert.assertEquals(configService.history(path, 2).get().size(), 2);
        Assert.assertEquals(configService.history(path, 2).get().stream().map(ConfigFileHistory::id).collect(Collectors.toList()),
                new ArrayList<>(Arrays.asList(configIdUpdate2, configIdUpdate1)));
    }

    @Test
    public void testGetAndSetDefaultOversizeConfigFile() throws ExecutionException, InterruptedException {
        Path path = Paths.get("/tmt/binary-files/trombone_hcd/app.bin");
        configService.create(path, ConfigData.fromString(configValue1), true, "some comment").get();
        Assert.assertEquals(configService.get(path).get().get().toJStringF(mat).get(), configValue1);

        ConfigId configIdUpdate1 = configService.update(path, ConfigData.fromString(configValue2), "Updated config to assembly").get();
        configService.update(path, ConfigData.fromString(configValue3), "Updated config").get();
        Assert.assertEquals(configService.getDefault(path).get().get().toJStringF(mat).get(), configValue3);

        configService.setDefault(path, Optional.of(configIdUpdate1)).get();
        Assert.assertEquals(configService.getDefault(path).get().get().toJStringF(mat).get(), configValue2);

        configService.setDefault(path, Optional.empty()).get();
        Assert.assertEquals(configService.getDefault(path).get().get().toJStringF(mat).get(), configValue3);
    }

    @Test
    public void testRetrieveVersionBasedOnDateForOverSizedFile() throws ExecutionException, InterruptedException {
        Path path = Paths.get("/test.conf");
        configService.create(path, ConfigData.fromString(configValue1), true, "commit initial oversize configuration").get();
        Assert.assertEquals(configService.get(path).get().get().toJStringF(mat).get(), configValue1);

        configService.update(path, ConfigData.fromString(configValue2), "updated config to assembly").get();
        Instant instant = Instant.now();
        configService.update(path, ConfigData.fromString(configValue3), "updated config to assembly").get();

        Assert.assertEquals(configService.get(path).get().get().toJStringF(mat).get(), configValue3);
        Assert.assertEquals(configService.get(path, instant).get().get().toJStringF(mat).get(), configValue2);
    }

}
