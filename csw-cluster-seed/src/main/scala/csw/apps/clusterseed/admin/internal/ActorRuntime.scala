package csw.apps.clusterseed.admin.internal

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown, Scheduler}
import akka.stream.{ActorMaterializer, Materializer}
import csw.services.BuildInfo
import csw.services.location.commons.ClusterAwareSettings
import csw.services.logging.internal.LoggingSystem
import csw.services.logging.scaladsl.LoggingSystemFactory

import scala.concurrent.{ExecutionContextExecutor, Future}

class ActorRuntime(_actorSystem: ActorSystem) {
  implicit val actorSystem: ActorSystem     = _actorSystem
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val mat: Materializer            = ActorMaterializer()
  implicit val scheduler: Scheduler         = actorSystem.scheduler

  val coordinatedShutdown = CoordinatedShutdown(actorSystem)

  def startLogging(): LoggingSystem =
    LoggingSystemFactory.start(BuildInfo.name, BuildInfo.version, ClusterAwareSettings.hostname, actorSystem)

  def shutdown(): Future[Done] = coordinatedShutdown.run()

}
