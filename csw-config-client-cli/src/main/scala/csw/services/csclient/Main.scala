package csw.services.csclient

import csw.services.BuildInfo
import csw.services.config.client.commons.ConfigClientLogger
import csw.services.csclient.cli.{ArgsParser, ClientCliWiring, Options}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.logging.scaladsl.{Logger, LoggingSystemFactory}

// $COVERAGE-OFF$
object Main extends App {
  val log: Logger = ConfigClientLogger.getLogger

  if (ClusterAwareSettings.seedNodes.isEmpty) {
    println(
      "clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
    )
  } else {
    ArgsParser.parse(args) match {
      case None          ⇒
      case Some(options) ⇒ run(options)
    }
  }

  private def run(options: Options): Unit = {
    val actorSystem = ClusterAwareSettings.system
    LoggingSystemFactory.start(BuildInfo.name, BuildInfo.version, ClusterAwareSettings.hostname, actorSystem)

    val wiring = new ClientCliWiring(actorSystem)
    try {
      wiring.cliApp.start(options)
    } finally {
      wiring.actorRuntime.shutdown()
    }
  }
}
// $COVERAGE-ON$
