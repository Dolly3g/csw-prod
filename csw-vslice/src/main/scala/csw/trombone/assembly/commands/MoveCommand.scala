package csw.trombone.assembly.commands

import akka.typed.ActorRef
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.messages.CommandExecutionResponse.{Completed, Error, NoLongerValid}
import csw.messages.CommandMessage.Submit
import csw.messages._
import csw.messages.ccs.CommandIssue.{RequiredHCDUnavailableIssue, WrongInternalStateIssue}
import csw.messages.ccs.commands.Setup
import csw.messages.params.models.RunId
import csw.messages.params.models.Units.encoder
import csw.trombone.assembly.MatcherResponse.{MatchCompleted, MatchFailed}
import csw.trombone.assembly._
import csw.trombone.assembly.actors.TromboneState.TromboneState
import csw.trombone.hcd.TromboneHcdState

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class MoveCommand(
    ctx: ActorContext[AssemblyCommandHandlerMsgs],
    ac: AssemblyContext,
    s: Setup,
    tromboneHCD: Option[ActorRef[SupervisorExternalMessage]],
    startState: TromboneState,
    stateActor: ActorRef[PubSub[AssemblyState]]
) extends AssemblyCommand(ctx, startState, stateActor) {

  import csw.trombone.assembly.actors.TromboneState._
  import ctx.executionContext
  val stagePosition   = s(ac.stagePositionKey)
  val encoderPosition = Algorithms.stagePositionToEncoder(ac.controlConfig.get, stagePosition.head)
  val stateMatcher    = Matchers.posMatcher(encoderPosition)
  val scOut = Setup(s.obsId, TromboneHcdState.axisMoveCK)
    .add(TromboneHcdState.positionKey -> encoderPosition withUnits encoder)

  def startCommand(): Future[CommandExecutionResponse] = {
    if (tromboneHCD.isEmpty)
      Future(NoLongerValid(s.runId, RequiredHCDUnavailableIssue(s"${ac.hcdComponentId} is not available")))
    else if (!(
               startState.cmdChoice == cmdUninitialized ||
               startState.moveChoice != moveIndexed && startState.moveChoice != moveMoving
             )) {
      Future(
        NoLongerValid(s.runId,
                      WrongInternalStateIssue(
                        s"Assembly state of ${startState.cmdChoice}/${startState.moveChoice} does not allow move"
                      ))
      )
    } else {
      publishState(TromboneState(cmdItem(cmdBusy), moveItem(moveMoving), startState.sodiumLayer, startState.nss))

      tromboneHCD.foreach(_ ! Submit(scOut, ctx.spawnAnonymous(Actor.ignore)))

      matchCompletion(stateMatcher, tromboneHCD.get, 5.seconds) {
        case MatchCompleted =>
          publishState(TromboneState(cmdItem(cmdReady), moveItem(moveIndexed), sodiumItem(false), startState.nss))
          Completed(s.runId)
        case MatchFailed(ex) =>
          println(s"Move command match failed with message: ${ex.getMessage}")
          Error(s.runId, ex.getMessage)
        case _ ⇒ Error(s.runId, "")
      }
    }
  }

  def stopCommand(): Unit = {
    tromboneHCD.foreach(_ ! Submit(TromboneHcdState.cancelSC(RunId(), s.obsId), ctx.spawnAnonymous(Actor.ignore)))
  }
}
