package csw.apps.containercmd

import java.nio.file.Path

import akka.actor.ActorSystem
import akka.typed.ActorRef
import com.typesafe.config.Config
import csw.apps.containercmd.cli.{ArgsParser, Options}
import csw.exceptions.{ClusterSeedsNotFound, UnableToParseOptions}
import csw.framework.internal.wiring.{Container, FrameworkWiring, Standalone}
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

import scala.async.Async.{async, await}
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

object ContainerCmd {

  /**
   * Utility for starting a Container to host components or start a component in Standalone mode.
   * @param name              The name to be used for the main app which uses this utility
   * @param args              The command line args accepted in the main app which uses this utility
   * @param defaultConfig     The default configuration which specifies the container or the component to be started
                              alone without any container
   * @return                  Actor ref of the container or supervisor of the component started without container
   */
  def start(name: String, args: Array[String], defaultConfig: Option[Config] = None): ActorRef[_] =
    new ContainerCmd(name, ClusterAwareSettings, true, defaultConfig).start(args)
}

private[containercmd] class ContainerCmd(
    name: String,
    clusterSettings: ClusterSettings,
    startLogging: Boolean,
    defaultConfig: Option[Config] = None
) {
  val log: Logger = new LoggerFactory(name).getLogger

  lazy val actorSystem: ActorSystem = clusterSettings.system
  lazy val wiring: FrameworkWiring  = FrameworkWiring.make(actorSystem)
  import wiring.actorRuntime._

  def start(args: Array[String]): ActorRef[_] = {
    if (clusterSettings.seedNodes.isEmpty)
      throw ClusterSeedsNotFound
    else
      new ArgsParser().parse(args) match {
        case None ⇒ throw UnableToParseOptions
        case Some(Options(standalone, isLocal, inputFilePath)) =>
          if (startLogging) wiring.actorRuntime.startLogging()

          log.debug(s"$name started with following arguments [${args.mkString(",")}]")

          try {
            val actorRef = Await.result(createF(standalone, isLocal, inputFilePath, defaultConfig), 30.seconds)
            log.info(s"Component is successfully created with actor actorRef $actorRef")
            actorRef
          } catch {
            case NonFatal(ex) ⇒
              log.error(s"${ex.getMessage}", ex = ex)
              shutdown()
              throw ex
          }
      }
  }

  private def createF(
      standalone: Boolean,
      isLocal: Boolean,
      inputFilePath: Option[Path],
      defaultConfig: Option[Config]
  ): Future[ActorRef[_]] = {
    async {
      val config   = await(wiring.configUtils.getConfig(isLocal, inputFilePath, defaultConfig))
      val actorRef = await(createComponent(standalone, wiring, config))
      log.info(s"Component is successfully created with actor actorRef $actorRef")
      actorRef
    }
  }

  private def createComponent(
      standalone: Boolean,
      wiring: FrameworkWiring,
      config: Config
  ): Future[ActorRef[_]] = {
    if (standalone) Standalone.spawn(config, wiring)
    else Container.spawn(config, wiring)
  }

  private def shutdown() = Await.result(wiring.actorRuntime.shutdown(), 10.seconds)
}
