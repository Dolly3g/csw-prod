package csw.common.framework.models

import enumeratum.{CirceEnum, Enum, EnumEntry}

import scala.collection.immutable

/**
 * Describes how a component uses the location service
 */
sealed abstract class LocationServiceUsage extends EnumEntry

object LocationServiceUsage extends Enum[LocationServiceUsage] with CirceEnum[LocationServiceUsage] {

  override def values: immutable.IndexedSeq[LocationServiceUsage] = findValues

  case object DoNotRegister            extends LocationServiceUsage
  case object RegisterOnly             extends LocationServiceUsage
  case object RegisterAndTrackServices extends LocationServiceUsage

  val JDoNotRegister: LocationServiceUsage            = DoNotRegister
  val JRegisterOnly: LocationServiceUsage             = RegisterOnly
  val JRegisterAndTrackServices: LocationServiceUsage = RegisterAndTrackServices
}
