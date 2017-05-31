package csw.services.location.scaladsl

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import csw.services.location.commons.{ClusterSettings, Constants, LocationServiceLogger}

/**
 * ActorSystemFactory creates a remote ActorSystem on the interface where csw-cluster is running. The ActorSystem starts on a
 * random port.
 *
 * @note It is highly recommended to create actors via this factory if it has to be registered with LocationService
 */
object ActorSystemFactory extends LocationServiceLogger.Simple {

  /**
   * Create an ActorSystem with `Constants.RemoteActorSystemName` as componentName
   */
  def remote(): ActorSystem = remote(Constants.RemoteActorSystemName)

  /**
   * Create an ActorSystem with the given name and remote properties
   *
   * @note Even if the custom configuration is provided for the given name of ActorSystem, it will be simply
   *       ignored, instead default remote configuration will be used while creating ActorSystem
   */
  def remote(componentName: String): ActorSystem = {
    val config = ConfigFactory
      .parseString(s"akka.remote.netty.tcp.hostname = ${ClusterSettings().hostname}")
      .withFallback(ConfigFactory.load().getConfig(Constants.RemoteActorSystemName))

    log.info("creating remote actor system")
    ActorSystem(componentName, config)
  }
}
