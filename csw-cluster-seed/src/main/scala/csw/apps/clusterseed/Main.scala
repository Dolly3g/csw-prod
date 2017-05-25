package csw.apps.clusterseed

import csw.apps.clusterseed.cli.{ArgsParser, Options}
import csw.apps.clusterseed.commons.ClusterSeedLogger
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings, CswCluster}

class Main(clusterSettings: ClusterSettings) {
  def start(args: Array[String]): Unit =
    new ArgsParser().parse(args).foreach {
      case Options(port) =>
        val updatedClusterSettings = clusterSettings.onPort(port)
        updatedClusterSettings.debug()
        CswCluster.withSettings(updatedClusterSettings)
    }
}

object Main extends App with ClusterSeedLogger.Simple {
  if (ClusterAwareSettings.seedNodes.isEmpty) {
    log.error(
      "clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
    )
  } else {
    new Main(ClusterAwareSettings).start(args)
  }
}
