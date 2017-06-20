package csw.apps.clusterseed.admin.internal

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import csw.apps.clusterseed.commons.ClusterSeedLogger
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.models.{FilterSet, LogMetadata}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol with ClusterSeedLogger.Simple {
  implicit val levelFormat: RootJsonFormat[Level] = new RootJsonFormat[Level] {
    override def write(obj: Level): JsValue = JsString(obj.name)

    override def read(json: JsValue): Level = json match {
      case JsString(value) ⇒ Level(value)
      case _ ⇒
        val runtimeException = new RuntimeException(s"can not parse $json")
        log.error(runtimeException.getMessage, runtimeException)
        throw runtimeException
    }
  }

  implicit val filterSetFormat: RootJsonFormat[FilterSet]     = jsonFormat1(FilterSet.apply)
  implicit val logMetadataFormat: RootJsonFormat[LogMetadata] = jsonFormat4(LogMetadata.apply)
}
