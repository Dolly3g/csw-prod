package csw.services.config.server

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.server.files._
import csw.services.config.server.http.{ConfigExceptionHandler, ConfigServiceRoute, HttpService}
import csw.services.config.server.svn.{SvnConfigService, SvnRepo}
import csw.services.location.commons.{ClusterSettings, CswCluster}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import wvlet.log.LogFormatter.AppLogFormatter
import wvlet.log.{FileHandler, LogFormatter, Logger}

class ServerWiring {
  Logger.setDefaultFormatter(LogFormatter.SourceCodeLogFormatter)
  Logger.setDefaultHandler(
      new FileHandler(
        fileName = "your-app.log", // Log file name
        formatter = AppLogFormatter // Any log formatter you like
      ))

  lazy val config: Config = ConfigFactory.load()
  lazy val settings       = new Settings(config)

  lazy val actorSystem   = ActorSystem("config-service", config)
  lazy val actorRuntime  = new ActorRuntime(actorSystem, settings)
  lazy val annexFileRepo = new AnnexFileRepo(actorRuntime.blockingIoDispatcher)
  lazy val svnRepo       = new SvnRepo(settings, actorRuntime.blockingIoDispatcher)

  lazy val annexFileService             = new AnnexFileService(settings, annexFileRepo, actorRuntime)
  lazy val configService: ConfigService = new SvnConfigService(settings, annexFileService, actorRuntime, svnRepo)

  lazy val clusterSettings                  = ClusterSettings()
  lazy val cswCluster: CswCluster           = CswCluster.withSettings(clusterSettings)
  lazy val locationService: LocationService = LocationServiceFactory.withCluster(cswCluster)

  lazy val configExceptionHandler = new ConfigExceptionHandler
  lazy val configServiceRoute     = new ConfigServiceRoute(configService, actorRuntime, configExceptionHandler)

  lazy val httpService: HttpService = new HttpService(locationService, configServiceRoute, settings, actorRuntime)
}

object ServerWiring {

  def make(_locationService: LocationService): ServerWiring = new ServerWiring {
    override lazy val locationService: LocationService = _locationService
  }

  def make(_clusterSettings: ClusterSettings): ServerWiring = new ServerWiring {
    override lazy val clusterSettings: ClusterSettings = _clusterSettings
  }

  def make(_clusterSettings: ClusterSettings, maybePort: Option[Int]): ServerWiring = new ServerWiring {
    override lazy val clusterSettings: ClusterSettings = _clusterSettings

    override lazy val settings: Settings = new Settings(config) {
      override val `service-port`: Int = maybePort.getOrElse(super.`service-port`)
    }
  }

  def make(_config: Config): ServerWiring = new ServerWiring {
    override lazy val config: Config = _config.withFallback(ConfigFactory.load())
  }
}
