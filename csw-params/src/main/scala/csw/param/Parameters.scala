package csw.param

import csw.param.parameters.{Key, KeyType, Parameter}

import scala.annotation.varargs
import scala.collection.JavaConverters._
import scala.language.implicitConversions

/**
 * Support for sets of generic, type-safe command or event parameters
 * (key/value objects with units)
 */
object Parameters {

  /**
   * Combines subsystem and the subsystem's prefix
   *
   * @param subsystem the subsystem that is the target of the command
   * @param prefix    the subsystem's prefix
   */
  case class Prefix(subsystem: Subsystem, prefix: String) {
    override def toString = s"[$subsystem, $prefix]"

    /**
     * Creates a Prefix from the given string
     *
     * @return a Prefix object parsed for the subsystem and prefix
     */
    def this(prefix: String) {
      this(Prefix.subsystem(prefix), prefix)
    }
  }

  /**
   * A top level key for a parameter gset: combines subsystem and the subsystem's prefix
   */
  object Prefix {
    private val SEPARATOR = '.'

    /**
     * Creates a Prefix from the given string
     *
     * @return an Prefix object parsed for the subsystem and prefix
     */
    implicit def stringToPrefix(prefix: String): Prefix = Prefix(subsystem(prefix), prefix)

    private def subsystem(keyText: String): Subsystem = {
      assert(keyText != null)
      Subsystem.lookup(keyText.splitAt(keyText.indexOf(SEPARATOR))._1).getOrElse(Subsystem.BAD)
    }
  }

  type ParameterSet = Set[Parameter[_]]

  /**
   * A trait to be mixed in that provides a parameter gset and prefix info
   */
  trait ParameterSetKeyData { self: ParameterSetType[_] =>

    /**
     * Returns an object providing the subsystem and prefix for the parameter gset
     */
    def prefix: Prefix

    /**
     * The subsystem for the parameter gset
     */
    final def subsystem: Subsystem = prefix.subsystem

    /**
     * The prefix for the parameter gset
     */
    final def prefixStr: String = prefix.prefix

    // This is the get for a Setup/Observe/Wait
    override def toString = s"$typeName([$subsystem, $prefixStr]$dataToString)"
  }

  /**
   * The base trait for various parameter gset types (commands or events)
   *
   * @tparam T the subclass of ParameterSetType
   */
  trait ParameterSetType[T <: ParameterSetType[T]] { self: T =>

    /**
     * A name identifying the type of parameter gset, such as "setup", "observe".
     * This is used in the JSON and toString output.
     */
    def typeName: String = getClass.getSimpleName

    /**
     * Holds the parameters for this parameter gset
     */
    def paramSet: ParameterSet

    /**
     * The number of parameters in this parameter gset
     *
     * @return the number of parameters in the parameter gset
     */
    def size: Int = paramSet.size

    /**
     * Adds a parameter to the parameter gset
     *
     * @param parameter the parameter to add
     * @tparam P the Parameter type
     * @return a new instance of this parameter gset with the given parameter added
     */
    def add[P <: Parameter[_]](parameter: P): T = doAdd(this, parameter)

    private def doAdd[P <: Parameter[_]](c: T, parameter: P): T = {
      val paramSetRemoved: T = removeByKeyname(c, parameter.keyName)
      create(paramSetRemoved.paramSet + parameter)
    }

    /**
     * Adds several parameters to the parameter gset
     *
     * @param parametersToAdd the list of parameters to add to the parameter gset
     * @tparam P must be a subclass of Parameter
     * @return a new instance of this parameter gset with the given parameter added
     */
    def madd[P <: Parameter[_]](parametersToAdd: P*): T =
      parametersToAdd.foldLeft(this)((c, parameter) => doAdd(c, parameter))

    /**
     * Returns an Option with the parameter for the key if found, otherwise None
     *
     * @param key the Key to be used for lookup
     * @return the parameter for the key, if found
     * @tparam S the Scala value type
     */
    def get[S](key: Key[S]): Option[Parameter[S]] = get(key.keyName, key.keyType)

    def get[S](keyName: String, keyType: KeyType[S]): Option[Parameter[S]] = {
      paramSet.find(p ⇒ p.keyName == keyName && p.keyType == keyType).asInstanceOf[Option[Parameter[S]]]
    }

    def find[S](parameter: Parameter[S]): Option[Parameter[S]] =
      get(parameter.keyName, parameter.keyType)

    /**
     * Return the parameter associated with a Key rather than an Option
     *
     * @param key the Key to be used for lookup
     * @tparam S the Scala value type
     * @return the parameter associated with the Key or a NoSuchElementException if the key does not exist
     */
    final def apply[S](key: Key[S]): Parameter[S] = get(key).get

    /**
     * Returns the actual parameter associated with a key
     *
     * @param key the Key to be used for lookup
     * @tparam S the Scala value type
     * @return the parameter associated with the key or a NoSuchElementException if the key does not exist
     */
    final def parameter[S](key: Key[S]): Parameter[S] = get(key).get

    /**
     * Returns true if the key exists in the parameter gset
     *
     * @param key the key to check for
     * @return true if the key is found
     * @tparam S the Scala value type
     */
    def exists[S](key: Key[S]): Boolean = get(key).isDefined

    /**
     * Remove a parameter from the parameter gset by key
     *
     * @param key the Key to be used for removal
     * @tparam S the Scala value type
     * @return a new T, where T is a parameter gset child with the key removed or identical if the key is not present
     */
    def remove[S](key: Key[S]): T = removeByKeyname(this, key.keyName) //doRemove(this, key)

    /**
     * Removes a parameter based on the parameter
     *
     * @param parameter to be removed from the parameter gset
     * @tparam P the type of the parameter to be removed
     * @return a new T, where T is a parameter gset child with the parameter removed or identical if the parameter is not present
     */
    def remove[P <: Parameter[_]](parameter: P): T = removeByParameter(this, parameter)

    /**
     * Function removes a parameter from the parameter gset c based on keyname
     *
     * @param c       the parameter gset to remove from
     * @param keyname the key name of the parameter to remove
     * @tparam P the Parameter type
     * @return a new T, where T is a parameter gset child with the parameter removed or identical if the parameter is not present
     */
    private def removeByKeyname[P <: Parameter[_]](c: ParameterSetType[T], keyname: String): T = {
      val f: Option[P] = getByKeyname(c.paramSet, keyname)
      f match {
        case Some(parameter) => create(c.paramSet.-(parameter))
        case None            => c.asInstanceOf[T] //create(c.parameters) also works
      }
    }

    /**
     * Function removes a parameter from the parameter gset c based on parameter content
     *
     * @param c           the parameter gset to remove from
     * @param parameterIn the parameter that should be removed
     * @tparam P the Parameter type
     * @return a new T, where T is a parameter gset child with the parameter removed or identical if the parameter is not presen
     */
    private def removeByParameter[P <: Parameter[_]](c: ParameterSetType[T], parameterIn: P): T = {
      val f: Option[P] = getByParameter(c.paramSet, parameterIn)
      f match {
        case Some(parameter) => create(c.paramSet.-(parameter))
        case None            => c.asInstanceOf[T]
      }
    }

    // Function to find a parameter by keyname - made public to enable matchers
    private def getByKeyname[P](parametersIn: ParameterSet, keyname: String): Option[P] =
      parametersIn.find(_.keyName == keyname).asInstanceOf[Option[P]]

    // Function to find a given parameter in the parameter gset
    private def getByParameter[P](parametersIn: ParameterSet, parameter: Parameter[_]): Option[P] =
      parametersIn.find(_.equals(parameter)).asInstanceOf[Option[P]]

    /**
     * Method called by subclass to create a copy with the same key (or other fields) and new parameters
     */
    protected def create(data: ParameterSet): T

    protected def dataToString: String = paramSet.mkString("(", ", ", ")")

    override def toString = s"$typeName$dataToString"

    /**
     * Returns true if the data contains the given key
     */
    def contains(key: Key[_]): Boolean = paramSet.exists(_.keyName == key.keyName)

    /**
     * Returns a gset containing the names of any of the given keys that are missing in the data
     *
     * @param keys one or more keys
     */
    def missingKeys(keys: Key[_]*): Set[String] = {
      val argKeySet        = keys.map(_.keyName).toSet
      val parametersKeySet = paramSet.map(_.keyName)
      argKeySet.diff(parametersKeySet)
    }

    /**
     * java API: Returns a gset containing the names of any of the given keys that are missing in the data
     *
     * @param keys one or more keys
     */
    @varargs
    def jMissingKeys(keys: Key[_]*): java.util.Set[String] = missingKeys(keys: _*).asJava

    /**
     * Returns a map based on this object where the keys and values are in string get
     * (Could be useful for exporting in a get that other languages can read).
     * Derived classes might want to add values to this map for fixed fields.
     */
    def getStringMap: Map[String, String] =
      paramSet.map(i => i.keyName -> i.values.map(_.toString).mkString(",")).toMap
  }

  /**
   * This will include information related to the observation that is tied to a parameter gset
   * This will grow and develop.
   *
   * @param obsId the observation id
   * @param runId unique ID for this parameter gset
   */
  case class CommandInfo(obsId: ObsId, runId: RunId = RunId()) {

    /**
     * Creates an instance with the given obsId and a unique runId
     */
    def this(obsId: String) = this(ObsId(obsId))
  }

  object CommandInfo {
    implicit def strToParamSetInfo(obsId: String): CommandInfo = CommandInfo(ObsId(obsId))
  }

  /**
   * Common trait for Setup, Observe and Wait commands
   */
  sealed trait Command {

    /**
     * A name identifying the type of parameter gset, such as "setup", "observe".
     * This is used in the JSON and toString output.
     */
    def typeName: String

    /**
     * information related to the parameter gset
     */
    val info: CommandInfo

    /**
     * identifies the target subsystem
     */
    val prefix: Prefix

    /**
     * an optional initial gset of parameters (keys with values)
     */
    val paramSet: ParameterSet
  }

  /**
   * Trait for sequence parameter sets
   */
  sealed trait SequenceCommand extends Command

  /**
   * Marker trait for control parameter sets
   */
  sealed trait ControlCommand extends Command

  /**
   * a parameter gset for setting telescope and instrument parameters
   *
   * @param info     information related to the parameter gset
   * @param prefix   identifies the target subsystem
   * @param paramSet an optional initial gset of parameters (keys with values)
   */
  case class Setup(info: CommandInfo, prefix: Prefix, paramSet: ParameterSet = Set.empty[Parameter[_]])
      extends ParameterSetType[Setup]
      with ParameterSetKeyData
      with SequenceCommand
      with ControlCommand {

    override def create(data: ParameterSet) = Setup(info, prefix, data)

    // This is here for Java to construct with String
    def this(info: CommandInfo, prefix: String) = this(info, Prefix.stringToPrefix(prefix))

    // The following overrides are needed for the Java API and javadocs
    // (Using a Java interface caused various Java compiler errors)
    override def add[P <: Parameter[_]](parameter: P): Setup = super.add(parameter)

    override def remove[S](key: Key[S]): Setup = super.remove(key)
  }

  /**
   * a parameter gset for setting observation parameters
   *
   * @param info     information related to the parameter gset
   * @param prefix   identifies the target subsystem
   * @param paramSet an optional initial gset of parameters (keys with values)
   */
  case class Observe(info: CommandInfo, prefix: Prefix, paramSet: ParameterSet = Set.empty[Parameter[_]])
      extends ParameterSetType[Observe]
      with ParameterSetKeyData
      with SequenceCommand
      with ControlCommand {

    override def create(data: ParameterSet) = Observe(info, prefix, data)

    // This is here for Java to construct with String
    def this(info: CommandInfo, prefix: String) = this(info, Prefix.stringToPrefix(prefix))

    // The following overrides are needed for the Java API and javadocs
    // (Using a Java interface caused various Java compiler errors)
    override def add[P <: Parameter[_]](parameter: P): Observe = super.add(parameter)

    override def remove[S](key: Key[S]): Observe = super.remove(key)
  }

  /**
   * a parameter gset indicating a pause in processing
   *
   * @param info     information related to the parameter gset
   * @param prefix   identifies the target subsystem
   * @param paramSet an optional initial gset of parameters (keys with values)
   */
  case class Wait(info: CommandInfo, prefix: Prefix, paramSet: ParameterSet = Set.empty[Parameter[_]])
      extends ParameterSetType[Wait]
      with ParameterSetKeyData
      with SequenceCommand {

    override def create(data: ParameterSet) = Wait(info, prefix, data)

    // This is here for Java to construct with String
    def this(info: CommandInfo, prefix: String) = this(info, Prefix.stringToPrefix(prefix))

    // The following overrides are needed for the Java API and javadocs
    // (Using a Java interface caused various Java compiler errors)
    override def add[P <: Parameter[_]](parameter: P): Wait = super.add(parameter)

    override def remove[S](key: Key[S]): Wait = super.remove(key)
  }

  /**
   * A parameters gset for returning results
   *
   * @param info     information related to the parameter gset
   * @param prefix   identifies the target subsystem
   * @param paramSet an optional initial gset of parameters (keys with values)
   */
  case class Result(info: CommandInfo, prefix: Prefix, paramSet: ParameterSet = Set.empty[Parameter[_]])
      extends ParameterSetType[Result]
      with ParameterSetKeyData {

    override def create(data: ParameterSet) = Result(info, prefix, data)

    // This is here for Java to construct with String
    def this(info: CommandInfo, prefix: String) = this(info, Prefix.stringToPrefix(prefix))

    // The following overrides are needed for the Java API and javadocs
    // (Using a Java interface caused various Java compiler errors)
    override def add[P <: Parameter[_]](parameter: P): Result = super.add(parameter)

    override def remove[S](key: Key[S]): Result = super.remove(key)
  }

  /**
   * Filters
   */
  object ParameterSetFilters {
    // A filter type for various parameter gset data
    type ParamSetFilter[A] = A => Boolean

    def prefixes(paramSets: Seq[ParameterSetKeyData]): Set[String] = paramSets.map(_.prefixStr).toSet

    def onlySetups(paramSets: Seq[SequenceCommand]): Seq[Setup] = paramSets.collect { case ct: Setup => ct }

    def onlyObserves(paramSets: Seq[SequenceCommand]): Seq[Observe] = paramSets.collect { case ct: Observe => ct }

    def onlyWaits(paramSets: Seq[SequenceCommand]): Seq[Wait] = paramSets.collect { case ct: Wait => ct }

    val prefixStartsWithFilter: String => ParamSetFilter[ParameterSetKeyData] = query =>
      sc => sc.prefixStr.startsWith(query)
    val prefixContainsFilter: String => ParamSetFilter[ParameterSetKeyData] = query =>
      sc => sc.prefixStr.contains(query)
    val prefixIsSubsystem: Subsystem => ParamSetFilter[ParameterSetKeyData] = query => sc => sc.subsystem.equals(query)

    def prefixStartsWith(query: String, paramSets: Seq[ParameterSetKeyData]): Seq[ParameterSetKeyData] =
      paramSets.filter(prefixStartsWithFilter(query))

    def prefixContains(query: String, paramSets: Seq[ParameterSetKeyData]): Seq[ParameterSetKeyData] =
      paramSets.filter(prefixContainsFilter(query))

    def prefixIsSubsystem(query: Subsystem, paramSets: Seq[ParameterSetKeyData]): Seq[ParameterSetKeyData] =
      paramSets.filter(prefixIsSubsystem(query))
  }

  /**
   * Contains a list of commands that can be sent to a sequencer
   */
  final case class CommandList(paramSets: Seq[SequenceCommand])

}
