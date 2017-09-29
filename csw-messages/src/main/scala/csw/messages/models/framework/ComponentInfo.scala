package csw.messages.models.framework

import ai.x.play.json.Jsonx
import csw.messages.TMTSerializable
import csw.messages.models.location.{ComponentType, Connection}
import play.api.libs.json._

import scala.collection.JavaConverters._
import scala.concurrent.duration.{DurationDouble, FiniteDuration}

/**
 * The information needed to create a component
 */
final case class ComponentInfo(
    name: String,
    componentType: ComponentType,
    prefix: String,
    behaviorFactoryClassName: String,
    locationServiceUsage: LocationServiceUsage,
    connections: Set[Connection] = Set.empty,
    initializeTimeout: FiniteDuration = 10.seconds,
    runTimeout: FiniteDuration = 10.seconds
) extends TMTSerializable {

  /**
   * Java API to get the list of connections for the assembly
   */
  def getConnections: java.util.List[Connection] = connections.toList.asJava
}

case object ComponentInfo {

  private def parseDuration(json: JsValue): JsResult[FiniteDuration] = json.validate[String].flatMap { str =>
    str.split(" ") match {
      case Array(length: String, unit: String) => JsSuccess(FiniteDuration.apply(length.toLong, unit))
      case _                                   => JsError("error.expected.duration.finite")
    }
  }

  implicit val finiteDurationReads: Reads[FiniteDuration]   = Reads[FiniteDuration](parseDuration)
  implicit val finiteDurationWrites: Writes[FiniteDuration] = Writes[FiniteDuration](d ⇒ Json.toJson(d.toString))

  implicit val componentInfoFormat: OFormat[ComponentInfo] = Jsonx.formatCaseClassUseDefaults[ComponentInfo]
}