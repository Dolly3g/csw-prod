package csw.services.config.client.internal

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import csw.services.location.commons.CswCoordinatedShutdown

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * A convenient class wrapping actor system and providing handles for execution context, materializer and clean up of actor system
 */
class ActorRuntime(_actorSystem: ActorSystem = ActorSystem()) {
  implicit val actorSystem: ActorSystem     = _actorSystem
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val mat: Materializer            = ActorMaterializer()

  def shutdown(): Future[Done] = CswCoordinatedShutdown.run(actorSystem)
}
