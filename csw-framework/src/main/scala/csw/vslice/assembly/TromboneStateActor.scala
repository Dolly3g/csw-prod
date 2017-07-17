package csw.vslice.assembly

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.param._
import csw.vslice.assembly.TromboneStateActor.{TromboneState, TromboneStateMsg}

class TromboneStateActor(ctx: ActorContext[TromboneStateMsg]) extends MutableBehavior[TromboneStateMsg] {

  import TromboneStateActor._

  var currentState: TromboneState = TromboneState(cmdDefault, moveDefault, sodiumLayerDefault, nssDefault)

  override def onMessage(msg: TromboneStateMsg): Behavior[TromboneStateMsg] = msg match {
    case SetState(tromboneState, replyTo) =>
      if (tromboneState != currentState) {
        ctx.system.eventStream.publish(tromboneState)
        currentState = tromboneState
        replyTo ! StateWasSet(true)
        this
      } else {
        replyTo ! StateWasSet(false)
        this
      }
    case GetState(replyTo) => {
      replyTo ! currentState
      this
    }
  }
}

class TromboneStateClient(ctx: ActorContext[TromboneState]) extends MutableBehavior[TromboneState] {

  import TromboneStateActor._

  ctx.system.eventStream.subscribe(ctx.self, classOf[TromboneState])

  private var internalState = defaultTromboneState

  override def onMessage(msg: TromboneState): Behavior[TromboneState] = msg match {
    case ts: TromboneState =>
      internalState = ts
      this
  }

  /**
   * The currentState as a TromonbeState is returned.
   * @return TromboneState current state
   */
  def currentState: TromboneState = internalState
}

object TromboneStateActor {

  def make(): Behavior[TromboneStateMsg] = Actor.mutable(ctx ⇒ new TromboneStateActor(ctx))

  // Keys for state telemetry item
  val cmdUninitialized               = Choice("uninitialized")
  val cmdReady                       = Choice("ready")
  val cmdBusy                        = Choice("busy")
  val cmdContinuous                  = Choice("continuous")
  val cmdError                       = Choice("error")
  val cmdKey                         = ChoiceKey("cmd", cmdUninitialized, cmdReady, cmdBusy, cmdContinuous, cmdError)
  val cmdDefault                     = cmdItem(cmdUninitialized)
  def cmd(ts: TromboneState): Choice = ts.cmd.head

  def cmdItem(ch: Choice): ChoiceParameter = cmdKey.set(ch)

  val moveUnindexed                   = Choice("unindexed")
  val moveIndexing                    = Choice("indexing")
  val moveIndexed                     = Choice("indexed")
  val moveMoving                      = Choice("moving")
  val moveKey                         = ChoiceKey("move", moveUnindexed, moveIndexing, moveIndexed, moveMoving)
  val moveDefault                     = moveItem(moveUnindexed)
  def move(ts: TromboneState): Choice = ts.move.head

  def moveItem(ch: Choice): ChoiceParameter = moveKey.set(ch)

  def sodiumKey                               = BooleanKey("sodiumLayer")
  val sodiumLayerDefault                      = sodiumItem(false)
  def sodiumLayer(ts: TromboneState): Boolean = ts.sodiumLayer.head

  def sodiumItem(flag: Boolean): BooleanParameter = sodiumKey.set(flag)

  def nssKey                          = BooleanKey("nss")
  val nssDefault                      = nssItem(false)
  def nss(ts: TromboneState): Boolean = ts.nss.head

  def nssItem(flag: Boolean): BooleanParameter = nssKey.set(flag)

  val defaultTromboneState = TromboneState(cmdDefault, moveDefault, sodiumLayerDefault, nssDefault)

  case class TromboneState(cmd: ChoiceParameter,
                           move: ChoiceParameter,
                           sodiumLayer: BooleanParameter,
                           nss: BooleanParameter)

  sealed trait TromboneStateMsg

  case class SetState(tromboneState: TromboneState, replyTo: ActorRef[StateWasSet]) extends TromboneStateMsg

  object SetState {

    def apply(cmd: ChoiceParameter,
              move: ChoiceParameter,
              sodiumLayer: BooleanParameter,
              nss: BooleanParameter,
              replyTo: ActorRef[StateWasSet]): SetState = SetState(TromboneState(cmd, move, sodiumLayer, nss), replyTo)

    def apply(cmd: Choice,
              move: Choice,
              sodiumLayer: Boolean,
              nss: Boolean,
              replyTo: ActorRef[StateWasSet]): SetState =
      SetState(TromboneState(cmdItem(cmd), moveItem(move), sodiumItem(sodiumLayer), nssItem(nss)), replyTo)
  }

  case class GetState(replyTo: ActorRef[TromboneState]) extends TromboneStateMsg

  case class StateWasSet(wasSet: Boolean) extends TromboneStateMsg

}
