package csw.services.ccs.internal.matchers

import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{KillSwitches, Materializer, OverflowStrategy}
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter._
import csw.messages.SupervisorCommonMessage.ComponentStateSubscription
import csw.messages.models.PubSub.Subscribe
import csw.messages.params.states.CurrentState
import csw.services.ccs.exceptions.MatchAborted
import csw.services.ccs.internal.matchers.MatcherResponse.{MatchCompleted, MatchFailed}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class Matcher(
    currentStateSource: ActorRef[ComponentStateSubscription],
    stateMatcher: StateMatcher
)(implicit ec: ExecutionContext, mat: Materializer) {

  def start: Future[MatcherResponse] = currentStateF.transform {
    case Success(_)  ⇒ Success(MatchCompleted)
    case Failure(ex) ⇒ Success(MatchFailed(ex))
  }

  def stop(): Unit = killSwitch.abort(MatchAborted(stateMatcher.prefix))

  private lazy val (killSwitch, currentStateF) = source
    .viaMat(KillSwitches.single)(Keep.right)
    .toMat(Sink.head)(Keep.both)
    .run()

  private def source =
    Source
      .actorRef[CurrentState](256, OverflowStrategy.fail)
      .mapMaterializedValue { ref ⇒
        currentStateSource ! ComponentStateSubscription(Subscribe(ref))
      }
      .filter(cs ⇒ cs.prefixStr == stateMatcher.prefix && stateMatcher.check(cs))
      .completionTimeout(stateMatcher.timeout.duration)
}
