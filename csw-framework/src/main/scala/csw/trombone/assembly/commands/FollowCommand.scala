package csw.trombone.assembly.commands

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.param.Events.EventTime
import csw.param.{BooleanParameter, DoubleParameter}
import csw.trombone.assembly.FollowActorMessages.{StopFollowing, UpdatedEventData}
import csw.trombone.assembly.FollowCommandMessages.{UpdateNssInUse, UpdateTromboneHcd, UpdateZAandFE}
import csw.trombone.assembly._
import csw.trombone.assembly.actors.{FollowActor, TromboneControl}
import csw.trombone.framework.RunningHcdMsg.Submit

class FollowCommand(ac: AssemblyContext,
                    initialElevation: DoubleParameter,
                    val nssInUseIn: BooleanParameter,
                    val tromboneHCDIn: Option[ActorRef[Submit]],
                    eventPublisher: Option[ActorRef[TrombonePublisherMsg]],
                    ctx: ActorContext[FollowCommandMessages])
    extends MutableBehavior[FollowCommandMessages] {

  val tromboneControl: ActorRef[TromboneControlMsg] =
    ctx.spawn(TromboneControl.behaviour(ac, tromboneHCDIn), "TromboneControl")

  var followActor: ActorRef[FollowActorMessages] = ctx.spawn(
    FollowActor.make(ac, initialElevation, nssInUseIn, Some(tromboneControl), eventPublisher, eventPublisher),
    "FollowActor"
  )

  val nssInUse: BooleanParameter = nssInUseIn

  var tromboneHCD: Option[ActorRef[Submit]] = tromboneHCDIn

  override def onMessage(msg: FollowCommandMessages): Behavior[FollowCommandMessages] = msg match {
    case UpdateNssInUse(nssInUseUpdate) =>
      if (nssInUseUpdate != nssInUse) {
        ctx.stop(followActor)
        followActor = ctx.spawnAnonymous(
          FollowActor.make(ac, initialElevation, nssInUseIn, Some(tromboneControl), eventPublisher, eventPublisher)
        )
      }
      this

    case UpdateZAandFE(zenithAngleIn, focusErrorIn) =>
      followActor ! UpdatedEventData(zenithAngleIn, focusErrorIn, EventTime())
      this

    case UpdateTromboneHcd(running) =>
      tromboneHCD = running
      tromboneControl ! TromboneControlMsg.UpdateTromboneHcd(running)
      this

    case StopFollowing =>
      ctx.stop(followActor)
      ctx.stop(ctx.self)
      this

    case m: FollowActorMessages =>
      followActor ! m
      this
  }

}

object FollowCommand {

  def make(assemblyContext: AssemblyContext,
           initialElevation: DoubleParameter,
           nssInUse: BooleanParameter,
           tromboneHCD: Option[ActorRef[Submit]],
           eventPublisher: Option[ActorRef[TrombonePublisherMsg]]): Behavior[FollowCommandMessages] =
    Actor.mutable(
      ctx ⇒ new FollowCommand(assemblyContext, initialElevation, nssInUse, tromboneHCD, eventPublisher, ctx)
    )
}
