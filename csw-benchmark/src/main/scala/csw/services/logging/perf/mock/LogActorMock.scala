package csw.services.logging.perf.mock

import com.persist.JsonOps.JsonObject
import csw.services.logging.RichMsg
import csw.services.logging.appenders.LogAppender
import csw.services.logging.commons.{Category, LoggingKeys, TMTDateTimeFormatter}
import csw.services.logging.internal.{Log, LoggingLevels}
import csw.services.logging.macros.SourceLocation
import csw.services.logging.scaladsl.noId

object LogActorMock {

  val noException = new Exception("No Exception")
  val standardHeaders: Map[String, RichMsg] =
    Map[String, RichMsg](LoggingKeys.HOST -> "hostname", LoggingKeys.NAME -> "test", LoggingKeys.VERSION -> "SNAPSHOT-1.0")

  val sourceLocation = SourceLocation("hcd.scala", "iris", "tromboneHCD", 12)
  val log = Log(
    Some("tromboneHCD"),
    LoggingLevels.DEBUG,
    noId,
    System.currentTimeMillis(),
    Some("testActor"),
    s"See if this is logged",
    Map.empty,
    sourceLocation,
    new Exception("No Exception")
  )

  def receiveLog(appender: LogAppender) = {
    var jsonObject = JsonObject(
      LoggingKeys.TIMESTAMP -> TMTDateTimeFormatter.format(log.time),
      LoggingKeys.MESSAGE   → log.msg,
      LoggingKeys.SEVERITY  -> log.level.name,
      LoggingKeys.CATEGORY  -> Category.Common.name
    )

    // This lime adds the user map objects as additional JsonObjects if the map is not empty
    jsonObject = jsonObject ++ log.map

    if (!log.sourceLocation.fileName.isEmpty) {
      jsonObject = jsonObject ++ JsonObject(LoggingKeys.FILE -> log.sourceLocation.fileName)
    }

    if (log.sourceLocation.line > 0)
      jsonObject = jsonObject ++ JsonObject(LoggingKeys.LINE -> log.sourceLocation.line)

    jsonObject = (log.sourceLocation.packageName, log.sourceLocation.className) match {
      case ("", "") ⇒ jsonObject
      case ("", c)  ⇒ jsonObject ++ JsonObject(LoggingKeys.CLASS -> c)
      case (p, c)   ⇒ jsonObject ++ JsonObject(LoggingKeys.CLASS -> s"$p.$c")
    }

    if (log.actorName.isDefined)
      jsonObject = jsonObject ++ JsonObject(LoggingKeys.ACTOR -> log.actorName.get)

    if (log.componentName.isDefined)
      jsonObject = jsonObject ++ JsonObject(LoggingKeys.COMPONENT_NAME -> log.componentName.get)

    if (log.ex != noException) jsonObject = jsonObject

    if (!log.kind.isEmpty)
      jsonObject = jsonObject ++ JsonObject(LoggingKeys.KIND -> log.kind)
    appender.append(jsonObject, Category.Common.name)
  }
}
