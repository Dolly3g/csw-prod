package csw.common.components

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.messages.ComponentMessage
import csw.messages.PubSub.{CommandStatePubSub, PublisherMessage}
import csw.messages.framework.ComponentInfo
import csw.messages.params.states.CurrentState
import csw.services.location.scaladsl.LocationService

import scala.concurrent.Future

class ComponentHandlerToSimulateFailure(
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    pubSubCommandState: ActorRef[CommandStatePubSub],
    locationService: LocationService
) extends SampleComponentHandlers(ctx, componentInfo, pubSubRef, pubSubCommandState, locationService) {

  override def onShutdown(): Future[Unit] = {
    throw new RuntimeException(ComponentHandlerToSimulateFailure.exceptionMsg)
  }
}

object ComponentHandlerToSimulateFailure {
  val exceptionMsg = "Shutdown failure occurred"
}
