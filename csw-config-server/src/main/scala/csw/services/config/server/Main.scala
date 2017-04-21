package csw.services.config.server

import akka.Done
import csw.services.config.server.cli.Options

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}

class Main {

  private val wiring = new ServerWiring
  import wiring._

  def start(args: Array[String]): Unit =
    configServerCliParser.parse(args).foreach {
      case Options(init, maybePort, clusterSeeds) =>
        sys.props("clusterSeeds") = clusterSeeds
        maybePort.foreach { port =>
          sys.props("httpPort") = port.toString
        }

        if (init) {
          wiring.svnRepo.initSvnRepo()
        }

        cswCluster.addJvmShutdownHook {
          Await.result(shutdown(), 10.seconds)
        }

        Await.result(httpService.registeredLazyBinding, 5.seconds)
    }

  def shutdown(): Future[Done] = httpService.shutdown()
}

object Main extends App {
  new Main().start(args)
}
