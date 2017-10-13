package csw.trombone.assembly.actors

import akka.typed.ActorRef
import csw.messages.{CommandExecutionResponse, CommandMessage, CommandResponse, PubSub}
import csw.trombone.assembly.commands.{AssemblyCommand, AssemblyState}

trait AssemblyCommandHandlers {
  var currentState: AssemblyState
  var currentCommand: AssemblyCommand
  var tromboneStateActor: ActorRef[PubSub[AssemblyState]]

  def onNotFollowing(commandMessage: CommandMessage): AssemblyCommandState

  def onFollowing(commandMessage: CommandMessage): AssemblyCommandState
  def onFollowingCommandComplete(replyTo: ActorRef[CommandResponse], result: CommandExecutionResponse): Unit

  def onExecuting(commandMessage: CommandMessage): AssemblyCommandState
  def onExecutingCommandComplete(replyTo: ActorRef[CommandResponse], result: CommandExecutionResponse): Unit
}
