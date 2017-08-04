package csw.common.framework.scaladsl

import akka.typed.scaladsl.ActorContext
import csw.common.ccs.Validation.Validation
import csw.common.framework.models.Component.ComponentInfo
import csw.common.framework.models.{CommandMsg, ComponentMsg}
import csw.common.framework.models.RunningMsg.DomainMsg

import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class ComponentHandlers[Msg <: DomainMsg: ClassTag](ctx: ActorContext[ComponentMsg],
                                                             componentInfo: ComponentInfo) {
  var isOnline: Boolean = false

  def initialize(): Future[Unit]
  def onRun(): Unit
  def onDomainMsg(msg: Msg): Unit
  def onControlCommand(commandMsg: CommandMsg): Validation
  def onShutdown(): Unit
  def onRestart(): Unit
  def onGoOffline(): Unit
  def onGoOnline(): Unit
}