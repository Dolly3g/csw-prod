package csw.services.logging.compat

import ch.qos.logback.core.spi.AppenderAttachable
import ch.qos.logback.core.{Appender, UnsynchronizedAppenderBase}
import csw.services.logging.internal.{LogSlf4j, MessageHandler}
import csw.services.logging.macros.DefaultSourceLocation
import csw.services.logging.noException
import csw.services.logging.scaladsl.{GenericLoggerFactory, Logger}

import scala.collection.mutable

/**
 *This class is used to allow the logs logged using SLF4J API to be routed to logback that in turn sends Slf4j messages to the common log.
 */
private[logging] class Slf4jAppender[E]() extends UnsynchronizedAppenderBase[E] with AppenderAttachable[E] {
  import csw.services.logging.internal.LoggingLevels._

  val log: Logger = GenericLoggerFactory.getLogger

  val appenders: mutable.HashSet[Appender[E]] = scala.collection.mutable.HashSet[Appender[E]]()

  def detachAndStopAllAppenders(): Unit = {}

  def detachAppender(x$1: String): Boolean = true

  def detachAppender(x$1: ch.qos.logback.core.Appender[E]): Boolean = true

  def getAppender(x$1: String): ch.qos.logback.core.Appender[E] = null

  def isAttached(x$1: ch.qos.logback.core.Appender[E]): Boolean = true

  def iteratorForAppenders(): java.util.Iterator[ch.qos.logback.core.Appender[E]] = null

  def addAppender(a: Appender[E]): Unit =
    appenders.add(a)

  def append(event: E): Unit =
    event match {
      case e: ch.qos.logback.classic.spi.LoggingEvent =>
        val frame = e.getCallerData()(0)
        val level = Level(e.getLevel.toString)
        val ex = try {
          val x = e.getThrowableProxy.asInstanceOf[ch.qos.logback.classic.spi.ThrowableProxy]
          x.getThrowable
        } catch {
          case ex: Any => noException
        }
        val msg =
          LogSlf4j(level, e.getTimeStamp, frame.getClassName, e.getFormattedMessage, frame.getLineNumber, frame.getFileName, ex)
        MessageHandler.sendMsg(msg)
      case x: Any =>
        log.warn(s"UNEXPECTED LOGBACK EVENT:${event.getClass}:$event")(() => DefaultSourceLocation)
    }
}
