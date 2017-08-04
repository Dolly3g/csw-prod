package csw.common.framework.internal

sealed trait ComponentMode
object ComponentMode {
  case object Idle        extends ComponentMode
  case object Initialized extends ComponentMode
  case object Running     extends ComponentMode
}
