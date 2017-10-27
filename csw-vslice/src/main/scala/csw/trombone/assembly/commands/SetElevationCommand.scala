package csw.trombone.assembly.commands

import akka.typed.ActorRef
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.messages.CommandMessage.Submit
import csw.messages._
import csw.messages.ccs.ValidationIssue.WrongInternalStateIssue
import csw.messages.ccs.commands.Setup
import csw.messages.params.models.Units.encoder
import csw.trombone.assembly._
import csw.trombone.assembly.actors.TromboneState.TromboneState
import csw.trombone.hcd.TromboneHcdState

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class SetElevationCommand(ctx: ActorContext[AssemblyCommandHandlerMsgs],
                          runId: String,
                          ac: AssemblyContext,
                          s: Setup,
                          tromboneHCD: Option[ActorRef[SupervisorExternalMessage]],
                          startState: TromboneState,
                          stateActor: ActorRef[PubSub[AssemblyState]])
    extends AssemblyCommand(ctx, "", startState, stateActor) {

  import TromboneHcdState._
  import csw.trombone.assembly.actors.TromboneState._
  import ctx.executionContext

  def startCommand(): Future[CommandExecutionResponse] = {
    if (startState.cmdChoice == cmdUninitialized || startState.moveChoice != moveIndexed && startState.moveChoice != moveMoving) {
      Future(
        NoLongerValid(runId,
                      WrongInternalStateIssue(
                        s"Assembly state of ${startState.cmdChoice}/${startState.moveChoice} does not allow datum"
                      ))
      )
    } else {
      val elevationItem   = s(ac.naElevationKey)
      val stagePosition   = Algorithms.rangeDistanceToStagePosition(elevationItem.head)
      val encoderPosition = Algorithms.stagePositionToEncoder(ac.controlConfig, stagePosition)

      println(
        s"Using elevation as rangeDistance: ${elevationItem.head} to get stagePosition: $stagePosition to encoder: $encoderPosition"
      )

      val stateMatcher = Matchers.posMatcher(encoderPosition)
      val scOut        = Setup(ac.commandInfo, axisMoveCK).add(positionKey -> encoderPosition withUnits encoder)

      publishState(TromboneState(cmdItem(cmdBusy), moveItem(moveIndexing), startState.sodiumLayer, startState.nss))
      tromboneHCD.foreach(_ ! Submit(scOut, ctx.spawnAnonymous(Actor.ignore)))
      matchCompletion(stateMatcher, tromboneHCD.get, 5.seconds) {
        case Completed(`runId`) =>
          publishState(TromboneState(cmdItem(cmdReady), moveItem(moveIndexed), sodiumItem(false), nssItem(false)))
          Completed(runId)
        case Error(`runId`, message) =>
          println(s"Data command match failed with error: $message")
          Error(runId, message)
        case _ ⇒ Error(runId, "")
      }
    }
  }

  def stopCommand(): Unit = {
    tromboneHCD.foreach(_ ! Submit(TromboneHcdState.cancelSC(s.info), ctx.spawnAnonymous(Actor.ignore)))
  }

}
