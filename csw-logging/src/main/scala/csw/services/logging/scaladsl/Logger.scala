package csw.services.logging.scaladsl
import csw.services.logging._
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.macros.SourceFactory

trait Logger {

  /**
   * Writes a trace level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id  optional id of a request
   */
  def trace(msg: => RichMsg, ex: Throwable = noException, id: AnyId = noId)(implicit factory: SourceFactory): Unit

  /**
   * Writes a debug level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id  optional id of a request
   */
  def debug(msg: => RichMsg, ex: Throwable = noException, id: AnyId = noId)(implicit factory: SourceFactory): Unit

  /**
   * Writes an info level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id  optional id of a request
   */
  def info(msg: => RichMsg, ex: Throwable = noException, id: AnyId = noId)(implicit factory: SourceFactory): Unit

  /**
   * Writes a warn level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id  optional id of a request
   */
  def warn(msg: => RichMsg, ex: Throwable = noException, id: AnyId = noId)(implicit factory: SourceFactory): Unit

  /**
   * Writes an error level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id  optional id of a request
   */
  def error(msg: => RichMsg, ex: Throwable = noException, id: AnyId = noId)(implicit factory: SourceFactory): Unit

  /**
   * Writes a fatal level log message.
   *
   * @param msg the message to be logged.
   * @param ex  an optional exception to be logged together with its stack trace.
   * @param id  optional id of a request
   */
  def fatal(msg: => RichMsg, ex: Throwable = noException, id: AnyId = noId)(implicit factory: SourceFactory): Unit

  /**
   * Write a log message to an alternative log.
   *
   * @param category the category for the message. For log files, this will be part of the file name. The following
   *                 categories are often used: server, client, gc, and time.
   * @param m        fields to be included in the log message.
   * @param ex       an optional exception to be logged together with its stack trace.
   * @param id       optional id of a request
   * @param time     the time to be written in the log. If not specified the default is the time this
   *                 method is called.
   */
  private[logging] def alternative(category: String,
                                   m: Map[String, RichMsg],
                                   ex: Throwable = noException,
                                   id: AnyId = noId,
                                   time: Long = System.currentTimeMillis()): Unit


  /**
    * Sets the log level of the component creating the Log
    */
  def setLogLevel(level :Level): Unit

}
