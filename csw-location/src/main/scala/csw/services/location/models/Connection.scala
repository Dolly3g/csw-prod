package csw.services.location.models

import acyclic.skipped
import csw.services.location.internal.ConnectionInfo
import csw.services.location.models.ConnectionType.{AkkaType, HttpType, TcpType}

/**
 * Represents a connection based on a componentId and the type of connection offered by the component
 */
sealed abstract class Connection(val connectionType: ConnectionType) extends TmtSerializable {

  type L <: Location

  /**
   * The component that is providing this connection
   */
  def componentId: ComponentId

  def connectionInfo: ConnectionInfo = ConnectionInfo(componentId.name, componentId.componentType, connectionType)

  /**
   * Creates a unique name for Connection based on Component name, ComponentType and ConnectionType
   */
  def name: String = connectionInfo.toString
}

abstract sealed class TypedConnection[T <: Location](connectionType: ConnectionType)
    extends Connection(connectionType) {
  override type L = T
}

object Connection {

  def from(input: String): Connection = input.split("-") match {
    case Array(name, componentType, connectionType) ⇒
      from(ConnectionInfo(name, ComponentType.withName(componentType), ConnectionType.withName(connectionType)))
    case _ ⇒ throw new IllegalArgumentException(s"Unable to parse '$input' to make Connection object")
  }

  def from(connectionInfo: ConnectionInfo): Connection = from(
    ComponentId(connectionInfo.name, connectionInfo.componentType),
    connectionInfo.connectionType
  )

  private def from(componentId: ComponentId, connectionType: ConnectionType): Connection = connectionType match {
    case AkkaType ⇒ AkkaConnection(componentId)
    case TcpType  ⇒ TcpConnection(componentId)
    case HttpType ⇒ HttpConnection(componentId)
  }

  /**
   * Represents a connection offered by remote Actors
   */
  case class AkkaConnection(componentId: ComponentId) extends TypedConnection[AkkaLocation](AkkaType)

  /**
   * Represents a http connection provided by the component
   */
  case class HttpConnection(componentId: ComponentId) extends TypedConnection[HttpLocation](HttpType)

  /**
   * represents a tcp connection provided by the component
   */
  case class TcpConnection(componentId: ComponentId) extends TypedConnection[TcpLocation](TcpType)

  import io.circe._
  import io.circe.syntax.EncoderOps

  implicit val connectionDecoder: Decoder[Connection] = ConnectionInfo.infoDecoder.map(Connection.from(_))
  implicit val connectionEncpder: Encoder[Connection] = (a: Connection) => a.connectionInfo.asJson
}
