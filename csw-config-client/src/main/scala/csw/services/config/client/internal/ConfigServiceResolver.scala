package csw.services.config.client.internal

import akka.http.scaladsl.model.Uri
import csw.services.config.client.commons.{ConfigClientLogger, ConfigServiceConnection}
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.Logger

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

/**
 * Resolves the location of server hosting the configuration service
 */
class ConfigServiceResolver(locationService: LocationService, actorRuntime: ActorRuntime) {
  val log: Logger = ConfigClientLogger.getLogger

  import actorRuntime.ec

  def uri: Future[Uri] = async {
    val location = await(locationService.resolve(ConfigServiceConnection.value, 5.seconds)).getOrElse(
      throw new RuntimeException(
        s"config service connection=${ConfigServiceConnection.value.name} can not be resolved"
      )
    )
    Uri(location.uri.toString)
  }
}
