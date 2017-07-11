package csw.vslice.hcd.mutable

import akka.NotUsed
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.param.Parameters.Setup
import csw.param.StateVariable.CurrentState
import csw.vslice.hcd.messages.FromComponentLifecycleMessage.Initialized
import csw.vslice.hcd.messages.InitialHcdMsg.{HcdResponse, Run, ShutdownComplete}
import csw.vslice.hcd.messages.RunningHcdMsg._
import csw.vslice.hcd.messages._
import csw.vslice.hcd.mutable.HcdActor.Context

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.reflect.ClassTag

object HcdActor {
  sealed trait Context
  object Context {
    case object Initial extends Context
    case object Running extends Context
  }
}

abstract class HcdActor[Msg <: DomainMsg: ClassTag](ctx: ActorContext[HcdMsg])(
    supervisor: ActorRef[FromComponentLifecycleMessage],
    pubSubRef: ActorRef[PubSub[CurrentState]]
) extends Actor.MutableBehavior[HcdMsg] {

  val wrapper: ActorRef[Msg] = ctx.spawnAdapter { x: Msg ⇒
    DomainHcdMsg(x)
  }

  import ctx.executionContext

  var context: Context = _

  def preStart(): Future[NotUsed]

  async {
    await(preStart())
    supervisor ! Initialized(ctx.self)
    context = Context.Initial
  }

  override def onMessage(msg: HcdMsg): Behavior[HcdMsg] = {
    (context, msg) match {
      case (Context.Initial, x: InitialHcdMsg) ⇒ handleInitial(x)
      case (Context.Running, x: RunningHcdMsg) ⇒ handleRuning(x)
      case _                                   ⇒ println(s"current context=$context does not handle message=$msg")
    }
    this
  }

  def handleInitial(x: InitialHcdMsg): Unit = x match {
    case Run(replyTo) =>
      onRun()
      context = Context.Running
      replyTo ! HcdResponse(ctx.self)
    case ShutdownComplete =>
      onShutdown()
  }

  def handleRuning(x: RunningHcdMsg): Unit = x match {
    case ShutdownComplete     => onShutdownComplete()
    case Lifecycle(message)   => handleLifecycle(message)
    case Submit(command)      => handleSetup(command)
    case GetPubSubActorRef    => PubSubRef(pubSubRef)
    case DomainHcdMsg(y: Msg) ⇒ handleTrombone(y)
    case DomainHcdMsg(y)      ⇒ println(s"unhandled domain msg: $y")
  }

  def onRun(): Unit
  def onShutdown(): Unit
  def onShutdownComplete(): Unit
  def handleLifecycle(x: ToComponentLifecycleMessage): Unit
  def handleSetup(sc: Setup): Unit
  def handleTrombone(msg: Msg): Unit
}