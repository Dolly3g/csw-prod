package csw.messages

import akka.actor.ActorSystem
import akka.typed.ActorRef
import csw.messages.PubSub.SubscriberMessage
import csw.messages.commands.{ControlCommand, Result}
import csw.messages.models.ccs.{Validation, ValidationIssue, Validations}
import csw.messages.models.framework.ComponentInfo
import csw.messages.models.location.TrackingEvent
import csw.messages.states.{ContainerLifecycleState, CurrentState, SupervisorLifecycleState}

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
  case object RunTimeout                                                     extends SupervisorIdleMessage
}

sealed trait FromComponentLifecycleMessage extends SupervisorIdleMessage
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

/**
 * Trait for a response message from an assembly to a submit or observe request
 */
sealed trait CommandResponse extends RunningMessage

sealed trait CommandExecutionResponse  extends CommandResponse
sealed trait CommandValidationResponse extends CommandResponse
object CommandValidationResponse {

  /**
   * Converts a validation result to a CommandStatus result
   *
   * @param validation the result of a validation either Validation.Valid or Validation.Invalid
   * @return cooresponding CommandStatus as CommandStatus.Valid or CommandStatus.Invalid with the identical issue
   */
  def validationAsCommandStatus(validation: Validation): CommandValidationResponse = {
    validation match {
      case Validations.Valid        => Accepted
      case inv: Validations.Invalid => Invalid(inv.issue)
    }
  }
}

/**
 * The configuration was not valid before starting
 * @param issue an issue that caused the input configuration to be invalid
 */
final case class Invalid(issue: ValidationIssue) extends CommandValidationResponse

object Invalid {
  // This is present to support returning a Validation as a CommandStatus
  def apply(in: Validations.Invalid): Invalid = new Invalid(in.issue)

  /**
   * Java API: This is present to support returning a Validation as a CommandStatus
   */
  def createInvalid(in: Validations.Invalid): Invalid = Invalid(in)
}

/**
 * The configuration was valid and started
 */
case object Accepted extends CommandValidationResponse

/**
 * Command Completed with a result
 * @param result - Result ParamSet to types in Configuration and use it here
 */
final case class CompletedWithResult(result: Result) extends CommandExecutionResponse

/**
 * The command was valid when received, but is no longer valid because of itervening activities
 */
final case class NoLongerValid(issue: ValidationIssue) extends CommandExecutionResponse

/**
 * The command has completed successfully
 */
case object Completed extends CommandExecutionResponse

/**
 * The command is currently executing or has not yet started
 * When used for a specific command, it indicates the command has not yet executed or is currently executing and is providing an update
 */
final case class InProgress(message: String = "") extends CommandExecutionResponse

/**
 * The command was started, but ended with error with the given message
 */
final case class Error(message: String) extends CommandExecutionResponse

/**
 * The command was aborted
 * Aborted means that the command/actions were stopped immediately.
 */
case object Aborted extends CommandExecutionResponse

/**
 * The command was cancelled
 * Cancelled means the command/actions were stopped at the next convenient place. This is usually appropriate for
 */
case object Cancelled extends CommandExecutionResponse

case class BehaviorChanged[T](ref: ActorRef[T]) extends CommandExecutionResponse
