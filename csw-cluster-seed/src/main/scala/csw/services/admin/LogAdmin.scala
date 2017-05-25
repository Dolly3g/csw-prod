package csw.services.admin

import csw.services.admin.exceptions.UnresolvedAkkaLocationException
import csw.services.location.models.{AkkaLocation, Connection}
import csw.services.location.scaladsl.LocationService
import csw.services.logging.internal.{GetComponentLogMetadata, SetComponentLogLevel}
import csw.services.logging.internal.LoggingLevels.Level
import akka.pattern.ask
import akka.util.Timeout
import csw.services.logging.models.LoggerMetadata

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class LogAdmin(locationService: LocationService, actorRuntime: ActorRuntime) {

  import actorRuntime._
  def getLogLevel(componentName: String): Future[LoggerMetadata] = async {
    implicit val timeout = Timeout(5.seconds)
    await(getLocation(componentName)) match {
      case Some(AkkaLocation(_, _, actorRef)) ⇒ await((actorRef ? GetComponentLogMetadata).mapTo[LoggerMetadata])
      case _                                  ⇒ throw UnresolvedAkkaLocationException(componentName)
    }
  }

  def setLogLevel(componentName: String, logLevel: Level): Future[Unit] = async {

    await(getLocation(componentName)) match {
      case Some(AkkaLocation(_, _, actorRef)) ⇒ actorRef ! SetComponentLogLevel(logLevel)
      case _                                  ⇒ throw UnresolvedAkkaLocationException(componentName)
    }
  }

  private def getLocation(componentName: String) = async {
    Connection.from(componentName) match {
      case connection: Connection ⇒ await(locationService.find(connection))
      case _                      ⇒ throw new IllegalArgumentException(s"$componentName is not a valid component name")
    }
  }
}