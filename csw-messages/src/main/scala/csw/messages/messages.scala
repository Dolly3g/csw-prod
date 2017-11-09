package csw.messages

import akka.actor.ActorSystem
import akka.typed.ActorRef
import csw.messages.PubSub.SubscriberMessage
import csw.messages.ccs.commands.{CommandResponse, ControlCommand}
import csw.messages.framework.{ComponentInfo, ContainerLifecycleState, SupervisorLifecycleState}
import csw.messages.location.TrackingEvent
import csw.messages.params.models.RunId
import csw.messages.params.states.CurrentState

/////////////

sealed trait PubSub[T]
object PubSub {
  sealed trait SubscriberMessage[T]           extends PubSub[T]
  case class Subscribe[T](ref: ActorRef[T])   extends SubscriberMessage[T]
  case class Unsubscribe[T](ref: ActorRef[T]) extends SubscriberMessage[T]

  sealed trait PublisherMessage[T] extends PubSub[T]
  case class Publish[T](data: T)   extends PublisherMessage[T]
}

///////////////

sealed trait ToComponentLifecycleMessage extends TMTSerializable
object ToComponentLifecycleMessage {
  case object GoOffline extends ToComponentLifecycleMessage
  case object GoOnline  extends ToComponentLifecycleMessage
}

///////////////

sealed trait ComponentMessage

sealed trait CommonMessage extends ComponentMessage
object CommonMessage {
  case class UnderlyingHookFailed(throwable: Throwable)          extends CommonMessage
  case class TrackingEventReceived(trackingEvent: TrackingEvent) extends CommonMessage
}

sealed trait IdleMessage extends ComponentMessage
object IdleMessage {
  case object Initialize extends IdleMessage
}

sealed trait CommandMessage extends RunningMessage {
  def command: ControlCommand
  def replyTo: ActorRef[CommandResponse]
}
object CommandMessage {
  case class Submit(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends CommandMessage
  case class Oneway(command: ControlCommand, replyTo: ActorRef[CommandResponse]) extends CommandMessage
}

sealed trait RunningMessage extends ComponentMessage with SupervisorRunningMessage
object RunningMessage {
  case class Lifecycle(message: ToComponentLifecycleMessage) extends RunningMessage with ContainerExternalMessage
  trait DomainMessage                                        extends RunningMessage
}

case object Shutdown extends SupervisorCommonMessage with ContainerCommonMessage
case object Restart  extends SupervisorCommonMessage with ContainerCommonMessage
////////////////////

sealed trait SupervisorMessage

sealed trait SupervisorExternalMessage extends SupervisorMessage with TMTSerializable
sealed trait SupervisorRunningMessage  extends SupervisorExternalMessage
sealed trait SupervisorRestartMessage  extends SupervisorMessage
object SupervisorRestartMessage {
  case object UnRegistrationComplete                    extends SupervisorRestartMessage
  case class UnRegistrationFailed(throwable: Throwable) extends SupervisorRestartMessage
}

sealed trait SupervisorCommonMessage extends SupervisorExternalMessage
object SupervisorCommonMessage {
  case class LifecycleStateSubscription(subscriberMessage: SubscriberMessage[LifecycleStateChanged])
      extends SupervisorCommonMessage
  case class ComponentStateSubscription(subscriberMessage: SubscriberMessage[CurrentState])
      extends SupervisorCommonMessage
  case class GetSupervisorLifecycleState(replyTo: ActorRef[SupervisorLifecycleState]) extends SupervisorCommonMessage
}

sealed trait SupervisorIdleMessage extends SupervisorMessage
object SupervisorIdleMessage {
  case class RegistrationSuccess(componentRef: ActorRef[RunningMessage])     extends SupervisorIdleMessage
  case class RegistrationNotRequired(componentRef: ActorRef[RunningMessage]) extends SupervisorIdleMessage
  case class RegistrationFailed(throwable: Throwable)                        extends SupervisorIdleMessage
  case object InitializeTimeout                                              extends SupervisorIdleMessage
}

sealed trait FromComponentLifecycleMessage extends SupervisorIdleMessage with SupervisorRunningMessage
object FromComponentLifecycleMessage {
  case class Running(componentRef: ActorRef[RunningMessage]) extends FromComponentLifecycleMessage
}

///////////////////
sealed trait ContainerMessage

sealed trait ContainerExternalMessage extends ContainerMessage with TMTSerializable

sealed trait ContainerCommonMessage extends ContainerExternalMessage
object ContainerCommonMessage {
  case class GetComponents(replyTo: ActorRef[Components])                           extends ContainerCommonMessage
  case class GetContainerLifecycleState(replyTo: ActorRef[ContainerLifecycleState]) extends ContainerCommonMessage
}

sealed trait ContainerIdleMessage extends ContainerMessage
object ContainerIdleMessage {
  case class SupervisorsCreated(supervisors: Set[SupervisorInfo]) extends ContainerIdleMessage
}

sealed trait FromSupervisorMessage extends ContainerIdleMessage
object FromSupervisorMessage {
  case class SupervisorLifecycleStateChanged(supervisor: ActorRef[SupervisorExternalMessage],
                                             supervisorLifecycleState: SupervisorLifecycleState)
      extends FromSupervisorMessage
}

case class LifecycleStateChanged(publisher: ActorRef[SupervisorExternalMessage], state: SupervisorLifecycleState)
    extends TMTSerializable

case class Components(components: Set[Component]) extends TMTSerializable

case class Component(supervisor: ActorRef[SupervisorExternalMessage], info: ComponentInfo) extends TMTSerializable

case class SupervisorInfo(system: ActorSystem, component: Component)

////////////////

sealed trait CommandManagerMessages
object CommandManagerMessages {
  case class AddTo(runIdParent: RunId, runIdChild: RunId) extends CommandManagerMessages
}

sealed trait CommandStatusMessages
object CommandStatusMessages {
  case class Query(runId: RunId, replyTo: ActorRef[CommandResponse])
      extends CommandStatusMessages
      with SupervisorExternalMessage
  case class Subscribe(runId: RunId, replyTo: ActorRef[CommandResponse])
      extends CommandStatusMessages
      with SupervisorExternalMessage
  case class UnSubscribe(runId: RunId, replyTo: ActorRef[CommandResponse])
      extends CommandStatusMessages
      with SupervisorExternalMessage
}

case class Update(runId: RunId, data: CommandResponse) extends CommandStatusMessages with CommandManagerMessages
case class Add(runId: RunId, actorRef: ActorRef[CommandResponse])
    extends CommandStatusMessages
    with CommandManagerMessages
