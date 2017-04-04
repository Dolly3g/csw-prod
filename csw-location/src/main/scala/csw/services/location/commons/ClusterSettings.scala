package csw.services.location.commons

import com.typesafe.config.{Config, ConfigFactory}
import csw.services.location.internal.Networks

import scala.collection.JavaConverters._

/**
  * ClusterSettings manages [[com.typesafe.config.Config]] values required by an [[akka.actor.ActorSystem]] to boot. It configures mainly
  * four parameters of an `ActorSystem`, namely :
  *
  *  - name (Name is defaulted to a constant value so that ActorSystem joins the cluster while booting)
  *  - akka.remote.netty.tcp.hostname (The hostname to boot an ActorSystem on)
  *  - akka.remote.netty.tcp.port     (The port to boot an ActorSystem on)
  *  - akka.cluster.seed-nodes        (Seed Nodes of the cluster)
  *
  * ClusterSettings require three values namely :
  *  - interfaceName (The network interface where cluster is formed.)
  *  - clusterSeed (The host address of the seedNode of the cluster)
  *  - isSeed (Claim self to be the seed of the cluster)
  *
  * The config values of the `ActorSystem` will be evaluated based on the above three settings as follows :
  *  - `akka.remote.netty.tcp.hostname` will be ipV4 address based on `interfaceName` from [[csw.services.location.internal.Networks]]
  *  - `akka.remote.netty.tcp.port` will be a random port or if `isSeed` is true then 3552 (Since cluster seeds will always
  * run on 3552)
  *  - `akka.cluster.seed-nodes` will be self if `isSeed` is true otherwise `clusterSeed` value will be used
  *
  * If none of the settings are provided then defaults will be picked as follows :
  *  - `akka.remote.netty.tcp.hostname` will be ipV4 address from [[csw.services.location.internal.Networks]]
  *  - `akka.remote.netty.tcp.port` will be a random port
  *  - `akka.cluster.seed-nodes` will be empty
  * and an `ActorSystem` will be created and a cluster will be formed with no Seed Nodes. It will also self join the cluster.
  *
  * `ClusterSettings` can be given in three ways :
  *  - by using the api
  *  - by providing system properties
  *  - or by providing environment variables
  *
  * If a `ClusterSettings` value e.g. isSeed is provided by more than one ways, then the precedence of consumption will be first from
  *  - System Properties
  *  - then from Environment variable
  *  - and then from `ClusterSettings` api
  *
  * @note Although `ClusterSettings` can be added through multiple ways, it is recommended that
  *       - `clusterSeed` is provided via environment variable,
  *       - `isSeed` is provided via system properties,
  *       - `interfaceName` is provide via environment variable and
  *       - the `ClusterSettings` api of providing values should be used for testing purpose only.
  *
  */
case class ClusterSettings(clusterName: String = Constants.ClusterName, values: Map[String, Any] = Map.empty) {
  val InterfaceNameKey = "interfaceName"
  val ClusterSeedKey = "clusterSeed"
  val IsSeedKey = "isSeed"

  def withEntry(key: String, value: Any): ClusterSettings = copy(values = values + (key → value))

  def withInterface(name: String): ClusterSettings = withEntry(InterfaceNameKey, name)

  def joinNode(seed: String): ClusterSettings = withEntry(ClusterSeedKey, seed)

  /**
    * Joins the cluster with seed running on localhost
    */
  def joinLocal(port: Int = 3552): ClusterSettings = withEntry(ClusterSeedKey, s"$hostname:$port")

  def asSeed: ClusterSettings = withEntry(IsSeedKey, "true")

  private lazy val allValues = values ++ sys.env ++ sys.props

  def hostname: String = {
    val interfaceName: String = allValues.getOrElse(InterfaceNameKey, "").toString
    new Networks(interfaceName).hostname()
  }

  def isSeed: Boolean = allValues.get(IsSeedKey).contains("true")

  def port: Int = if (isSeed) 3552 else 0

  def seedNodes: List[String] = allValues.get(ClusterSeedKey) match {
    case Some(seed)     ⇒ List(s"akka.tcp://$clusterName@$seed")
    case None if isSeed ⇒ List(s"akka.tcp://$clusterName@$hostname:3552")
    case None           ⇒ List.empty
  }

  def config: Config = {
    val computedValues: Map[String, Any] = Map(
      "akka.remote.netty.tcp.hostname" → hostname,
      "akka.remote.netty.tcp.port" → port,
      "akka.cluster.seed-nodes" → seedNodes.asJava
    )

    ConfigFactory
      .parseMap(computedValues.asJava)
      .withFallback(ConfigFactory.load().getConfig(clusterName))
  }
}