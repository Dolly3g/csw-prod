package csw.messages.location

import csw.messages.{ContainerExternalMessage, SupervisorExternalMessage, TMTSerializable}
import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import scala.collection.immutable.IndexedSeq

/**
 * Represents a type of the Component. It should be serializable since it has to be transmittable over the network.
 * The type will always be represented in lower case.
 */
sealed abstract class ComponentType(val messageManifest: String) extends EnumEntry with Lowercase with TMTSerializable {

  /**
   * The name of ComponentType e.g. for HCD components, the name will be represented as `hcd`.
   */
  def name: String = entryName
}

object ComponentType extends Enum[ComponentType] with PlayJsonEnum[ComponentType] {

  def values: IndexedSeq[ComponentType] = findValues

  /**
   * Represents a container for components e.g assemblies and HCDs
   */
  case object Container extends ComponentType(classOf[ContainerExternalMessage].getSimpleName)

  /**
   * Represents a component that controls a hardware device
   */
  case object HCD extends ComponentType(classOf[SupervisorExternalMessage].getSimpleName)

  /**
   * Represents a component that controls one or more HCDs or assemblies
   */
  case object Assembly extends ComponentType(classOf[SupervisorExternalMessage].getSimpleName)

  /**
   * Represents a general purpose service component e.g. actor and/or web service application
   */
  case object Service extends ComponentType("")

}
