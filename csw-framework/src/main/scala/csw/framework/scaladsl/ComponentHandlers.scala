package csw.framework.scaladsl

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.messages.PubSub.PublisherMessage
import csw.messages.RunningMessage.DomainMessage
import csw.messages.ccs.Validation
import csw.messages.ccs.commands.ControlCommand
import csw.messages.framework.ComponentInfo
import csw.messages.location.TrackingEvent
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponse, ComponentMessage}
import csw.services.location.scaladsl.LocationService

import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class ComponentHandlers[Msg <: DomainMessage: ClassTag](
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService
) {
  var isOnline: Boolean = false

  def initialize(): Future[ComponentHandlers[Msg]]
  def onLocationTrackingEvent(trackingEvent: TrackingEvent): ComponentHandlers[Msg]
  def onDomainMsg(msg: Msg): ComponentHandlers[Msg]
  def onSubmit(controlCommand: ControlCommand, replyTo: ActorRef[CommandResponse]): (ComponentHandlers[Msg], Validation)
  def onOneway(controlCommand: ControlCommand): (ComponentHandlers[Msg], Validation)
  def onShutdown(): Future[Unit]
  def onGoOffline(): ComponentHandlers[Msg]
  def onGoOnline(): ComponentHandlers[Msg]
}
