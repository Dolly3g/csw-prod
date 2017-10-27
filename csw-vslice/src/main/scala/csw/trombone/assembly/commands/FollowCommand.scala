package csw.trombone.assembly.commands

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.messages._
import csw.messages.ccs.ValidationIssue.WrongInternalStateIssue
import csw.messages.ccs.commands.Setup
import csw.trombone.assembly.actors.TromboneState.{TromboneState, _}
import csw.trombone.assembly.{AssemblyCommandHandlerMsgs, AssemblyContext}

import scala.concurrent.Future

class FollowCommand(ctx: ActorContext[AssemblyCommandHandlerMsgs],
                    runId: String,
                    ac: AssemblyContext,
                    s: Setup,
                    tromboneHCD: Option[ActorRef[SupervisorExternalMessage]],
                    startState: TromboneState,
                    stateActor: ActorRef[PubSub[AssemblyState]])
    extends AssemblyCommand(ctx, runId, startState, stateActor) {

  import ctx.executionContext

  override def startCommand(): Future[CommandExecutionResponse] = {
    if (startState.cmdChoice == cmdUninitialized
        || startState.moveChoice != moveIndexed && startState.moveChoice != moveMoving
        || !startState.sodiumLayerValue) {
      Future(
        NoLongerValid(
          runId,
          WrongInternalStateIssue(
            s"Assembly state of ${startState.cmdChoice}/${startState.moveChoice}/${startState.sodiumLayerValue} does not allow follow"
          )
        )
      )
    } else {
      publishState(
        TromboneState(cmdItem(cmdContinuous),
                      moveItem(moveMoving),
                      sodiumItem(startState.sodiumLayerValue),
                      nssItem(s(ac.nssInUseKey).head))
      )
      Future(Completed(runId))
    }
  }

  override def stopCommand(): Unit = {}

}
