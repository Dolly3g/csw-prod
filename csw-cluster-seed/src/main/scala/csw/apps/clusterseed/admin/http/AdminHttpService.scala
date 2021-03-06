package csw.apps.clusterseed.admin.http

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import csw.apps.clusterseed.admin.internal.{ActorRuntime, Settings}
import csw.apps.clusterseed.commons.ClusterSeedLogger
import csw.services.location.commons.ClusterAwareSettings
import csw.services.logging.scaladsl.Logger

import scala.async.Async._
import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * Initialise AdminServer
 */
class AdminHttpService(adminRoutes: AdminRoutes, actorRuntime: ActorRuntime, settings: Settings) {
  val log: Logger = ClusterSeedLogger.getLogger

  import actorRuntime._

  lazy val registeredLazyBinding: Future[ServerBinding] = async {
    val binding = await(bind())
    log.info(s"Server online at http://${binding.localAddress.getHostName}:${binding.localAddress.getPort}/")
    binding
  } recoverWith {
    case NonFatal(ex) ⇒
      log.error("can not start admin http server", ex = ex)
      shutdown().map(_ ⇒ throw ex)
  }

  private def bind() = Http().bindAndHandle(
    handler = adminRoutes.route,
    interface = ClusterAwareSettings.hostname,
    port = settings.`admin-port`
  )
}
