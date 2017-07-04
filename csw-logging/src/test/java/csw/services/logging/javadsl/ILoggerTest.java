package csw.services.logging.javadsl;

import akka.actor.ActorSystem;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import csw.services.logging.appenders.LogAppenderBuilder;
import csw.services.logging.components.JTromboneHCD;
import csw.services.logging.components.JTromboneHCD1;
import csw.services.logging.internal.LoggingLevels;
import csw.services.logging.internal.LoggingSystem;
import csw.services.logging.utils.LogUtil;
import csw.services.logging.utils.TestAppender;
import org.junit.*;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ILoggerTest {
    private static ActorSystem actorSystem = ActorSystem.create("base-system");
    private static LoggingSystem loggingSystem;

    private static List<JsonObject> logBuffer = new ArrayList<>();

    private static JsonObject parse(String json) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonElement.class).getAsJsonObject();
        return jsonObject;
    }

    private static TestAppender testAppender     = new TestAppender(x -> {
        logBuffer.add(parse(x.toString()));
        return null;
    });
    private static List<LogAppenderBuilder> appenderBuilders = Arrays.asList(testAppender);

    @BeforeClass
    public static void setup() {
        loggingSystem = JLoggingSystemFactory.start("Logger-Test", "SNAPSHOT-1.0", "localhost", actorSystem, appenderBuilders);
    }

    @After
    public void afterEach() {
        logBuffer.clear();
    }

    @AfterClass
    public static void teardown() throws Exception {
        loggingSystem.javaStop().get();
        Await.result(actorSystem.terminate(), Duration.create(10, TimeUnit.SECONDS));
    }

    @Test
    public void testDefaultLogConfigurationAndFilter() throws InterruptedException {
        JTromboneHCD jTromboneHCD = new JTromboneHCD();
        JTromboneHCD1 jTromboneHCD1 = new JTromboneHCD1();
        String tromboneHcdClassName = jTromboneHCD.getClass().getName();

        jTromboneHCD.startLogging();
        jTromboneHCD1.startLogging();

        System.out.println("-------------------------------");
        jTromboneHCD1.setLevel();
        System.out.println("---------------HCD Start----------------");
        jTromboneHCD.startLogging();
        System.out.println("---------------HCD END----------------");
        System.out.println("---------------HCD1 Start----------------");
        jTromboneHCD1.startLogging();
        System.out.println("---------------HCD1 END----------------");

        Thread.sleep(300);

        logBuffer.forEach(log -> System.out.println(log));
        logBuffer.forEach(log -> {
            Assert.assertEquals("tromboneHcd", log.get("@componentName").getAsString());

            Assert.assertTrue(log.has("@severity"));
            String severity = log.get("@severity").getAsString().toLowerCase();

            Assert.assertEquals(LogUtil.logMsgMap.get(severity), log.get("message").getAsString());
            Assert.assertEquals(tromboneHcdClassName, log.get("class").getAsString());

            LoggingLevels.Level currentLogLevel = LoggingLevels.Level$.MODULE$.apply(severity);
            Assert.assertTrue(currentLogLevel.$greater$eq(LoggingLevels.DEBUG$.MODULE$));
        });
    }

}
