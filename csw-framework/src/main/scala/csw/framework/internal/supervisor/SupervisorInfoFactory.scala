package csw.framework.internal.supervisor

import akka.typed.ActorRef
import csw.framework.internal.pubsub.PubSubBehaviorFactory
import csw.framework.internal.wiring.CswFrameworkSystem
import csw.messages.ContainerIdleMessage
import csw.messages.framework.ComponentInfo
import csw.messages.models.{Component, SupervisorInfo}
import csw.services.location.commons.ActorSystemFactory
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

import scala.async.Async._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.control.NonFatal

/**
 * The factory for creating supervisor actors of a component specified by [[csw.messages.framework.ComponentInfo]]
 */
class SupervisorInfoFactory(containerName: String) {
  val log: Logger = new LoggerFactory(containerName).getLogger

  def make(
      containerRef: ActorRef[ContainerIdleMessage],
      componentInfo: ComponentInfo,
      locationService: LocationService,
      registrationFactory: RegistrationFactory
  ): Future[Option[SupervisorInfo]] = {
    val system                                = ActorSystemFactory.remote(s"${componentInfo.name}-system")
    implicit val ec: ExecutionContextExecutor = system.dispatcher
    val richSystem                            = new CswFrameworkSystem(system)
    val pubSubBehaviorFactory                 = new PubSubBehaviorFactory

    async {
      val supervisorBehavior = {
        SupervisorBehaviorFactory.make(
          Some(containerRef),
          componentInfo,
          locationService,
          registrationFactory,
          pubSubBehaviorFactory
        )
      }
      val actorRefF = richSystem.spawnTyped(supervisorBehavior, componentInfo.name)
      Some(SupervisorInfo(system, Component(await(actorRefF), componentInfo)))
    } recoverWith {
      case NonFatal(exception) ⇒
        async {
          log.error(s"Exception :[${exception.getMessage}] occurred while spawning supervisor: [${componentInfo.name}]",
                    ex = exception)
          await(system.terminate())
          None
        }
    }
  }
}
