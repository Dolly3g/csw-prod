package csw.framework.models

import akka.actor.ActorSystem
import akka.typed.ActorRef
import csw.ccs.CommandStatus.CommandResponse
import csw.framework.internal.container.ContainerLifecycleState
import csw.framework.internal.supervisor.SupervisorLifecycleState
import csw.framework.models.PubSub.SubscriberMessage
import csw.param.commands.ControlCommand
import csw.param.states.CurrentState
import csw.services.location.models.{RegistrationResult, TmtSerializable}

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

sealed trait ToComponentLifecycleMessage
object ToComponentLifecycleMessage {
  case object GoOffline extends ToComponentLifecycleMessage
  case object GoOnline  extends ToComponentLifecycleMessage
}

///////////////

sealed trait ComponentMessage

sealed trait CommonMessage extends ComponentMessage
object CommonMessage {
  case class UnderlyingHookFailed(throwable: Throwable) extends CommonMessage
}

sealed trait IdleMessage extends ComponentMessage
object IdleMessage {
  case object Initialize extends IdleMessage
}

sealed trait InitialMessage extends ComponentMessage
object InitialMessage {
  case object Run extends InitialMessage
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

case object Shutdown extends SupervisorCommonMessage with ContainerCommonMessage with ContainerExternalMessage
case object Restart  extends SupervisorCommonMessage with ContainerCommonMessage with ContainerExternalMessage

///////////////

sealed trait SupervisorMessage

sealed trait SupervisorExternalMessage extends SupervisorMessage with TmtSerializable
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
  case class RegistrationComplete(registrationResult: RegistrationResult, componentRef: ActorRef[InitialMessage])
      extends SupervisorIdleMessage
  case class RegistrationFailed(throwable: Throwable) extends SupervisorIdleMessage
  case object InitializeTimeout                       extends SupervisorIdleMessage
  case object RunTimeout                              extends SupervisorIdleMessage
}

sealed trait FromComponentLifecycleMessage extends SupervisorIdleMessage
object FromComponentLifecycleMessage {
  case class Initialized(componentRef: ActorRef[InitialMessage]) extends FromComponentLifecycleMessage
  case class Running(componentRef: ActorRef[RunningMessage])     extends FromComponentLifecycleMessage
}

///////////////

sealed trait ContainerMessage

sealed trait ContainerExternalMessage extends ContainerMessage with TmtSerializable

sealed trait ContainerCommonMessage extends ContainerMessage
object ContainerCommonMessage {
  case class RegistrationComplete(registrationResult: RegistrationResult) extends ContainerCommonMessage
  case class RegistrationFailed(throwable: Throwable)                     extends ContainerCommonMessage
  case class GetComponents(replyTo: ActorRef[Components])                 extends ContainerCommonMessage with ContainerExternalMessage
  case class GetContainerLifecycleState(replyTo: ActorRef[ContainerLifecycleState])
      extends ContainerCommonMessage
      with ContainerExternalMessage
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
    extends TmtSerializable

case class Components(components: Set[Component]) extends TmtSerializable

case class Component(supervisor: ActorRef[SupervisorExternalMessage], info: ComponentInfo)

case class SupervisorInfo(system: ActorSystem, component: Component)
