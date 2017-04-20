package csw.services.config.server.cli

class ConfigServerCliParser {
  val parser = new scopt.OptionParser[ConfiServiceCliOptions]("scopt") {
    head("config-sever", System.getProperty("CSW_VERSION"))

    opt[Unit]("init") action { (x, c) =>
      c.copy(init = true)
    } text "the repository is initialized, if it does not yet exist"

    opt[Int]("port") action { (x, c) =>
      c.copy(port = Some(x))
    } text "http port at which service will be run, if not specified value from config file will be used"

    opt[String]("clusterSeeds").required() action { (x, c) =>
      c.copy(clusterSeeds = x)
    } text "comma separated list of cluster-seed node addresses"

    help("help")

    version("version")
  }

  def parse(args: Seq[String]): Option[ConfiServiceCliOptions] = parser.parse(args, ConfiServiceCliOptions())
}
