package csw.messages.params.generics

import java.util

import csw.messages.TMTSerializable

import scala.annotation.varargs
import scala.collection.JavaConverters.setAsJavaSetConverter
import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.compat.java8.OptionConverters.RichOptionForJava8

/**
 * The base trait for various parameter set types (commands or events)
 *
 * @tparam T the subclass of ParameterSetType
 */
abstract class ParameterSetType[T <: ParameterSetType[T]] extends TMTSerializable { self: T =>

  /**
   * A name identifying the type of parameter set, such as "setup", "observe".
   * This is used in the JSON and toString output.
   */
  def typeName: String = getClass.getSimpleName

  /**
   * Holds the parameters for this parameter set
   */
  def paramSet: Set[Parameter[_]]
  def jParamSet: util.Set[Parameter[_]] = paramSet.asJava

  /**
   * The number of parameters in this parameter set
   *
   * @return the number of parameters in the parameter set
   */
  def size: Int = paramSet.size

  /**
   * Adds a parameter to the parameter set
   *
   * @param parameter the parameter to add
   * @tparam P the Parameter type
   * @return a new instance of this parameter set with the given parameter added
   */
  def add[P <: Parameter[_]](parameter: P): T = doAdd(this, parameter)

  private def doAdd[P <: Parameter[_]](c: T, parameter: P): T = {
    val paramSetRemoved: T = removeByKeyname(c, parameter.keyName)
    create(paramSetRemoved.paramSet + parameter)
  }

  /**
   * Adds several parameters to the parameter set
   *
   * @param parametersToAdd the list of parameters to add to the parameter set
   * @tparam P must be a subclass of Parameter
   * @return a new instance of this parameter set with the given parameter added
   */
  @varargs
  def madd[P <: Parameter[_]](parametersToAdd: P*): T = madd(parametersToAdd.toSet)

  def madd[P <: Parameter[_]](parametersToAdd: Set[P]): T =
    parametersToAdd.foldLeft(this)((c, parameter) => doAdd(c, parameter))

  /**
   * Returns an Option with the parameter for the key if found, otherwise None
   *
   * @param key the Key to be used for lookup
   * @return the parameter for the key, if found
   * @tparam S the Scala value type
   */
  def get[S](key: Key[S]): Option[Parameter[S]]         = get(key.keyName, key.keyType)
  def jGet[S](key: Key[S]): util.Optional[Parameter[S]] = get(key).asJava

  def get[S](keyName: String, keyType: KeyType[S]): Option[Parameter[S]] = {
    paramSet.find(p ⇒ p.keyName == keyName && p.keyType == keyType).asInstanceOf[Option[Parameter[S]]]
  }
  def jGet[S](keyName: String, keyType: KeyType[S]): util.Optional[Parameter[S]] = get(keyName, keyType).asJava

  def find[S](parameter: Parameter[S]): Option[Parameter[S]] =
    get(parameter.keyName, parameter.keyType)
  def jFind[S](parameter: Parameter[S]): util.Optional[Parameter[S]] = find(parameter).asJava

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
   * Returns true if the key exists in the parameter set
   *
   * @param key the key to check for
   * @return true if the key is found
   * @tparam S the Scala value type
   */
  def exists[S](key: Key[S]): Boolean = get(key).isDefined

  /**
   * Remove a parameter from the parameter set by key
   *
   * @param key the Key to be used for removal
   * @tparam S the Scala value type
   * @return a new T, where T is a parameter set child with the key removed or identical if the key is not present
   */
  def remove[S](key: Key[S]): T = removeByKeyname(this, key.keyName) //doRemove(this, key)

  /**
   * Removes a parameter based on the parameter
   *
   * @param parameter to be removed from the parameter set
   * @tparam P the type of the parameter to be removed
   * @return a new T, where T is a parameter set child with the parameter removed or identical if the parameter is not present
   */
  def remove[P <: Parameter[_]](parameter: P): T = removeByParameter(this, parameter)

  /**
   * Function removes a parameter from the parameter set c based on keyname
   *
   * @param c       the parameter set to remove from
   * @param keyname the key name of the parameter to remove
   * @tparam P the Parameter type
   * @return a new T, where T is a parameter set child with the parameter removed or identical if the parameter is not present
   */
  private def removeByKeyname[P <: Parameter[_]](c: ParameterSetType[T], keyname: String): T = {
    val f: Option[P] = getByKeyname(c.paramSet, keyname)
    f match {
      case Some(parameter) => create(c.paramSet - parameter)
      case None            => c.asInstanceOf[T] //create(c.parameters) also works
    }
  }

  /**
   * Function removes a parameter from the parameter set c based on parameter content
   *
   * @param c           the parameter set to remove from
   * @param parameterIn the parameter that should be removed
   * @tparam P the Parameter type
   * @return a new T, where T is a parameter set child with the parameter removed or identical if the parameter is not presen
   */
  private def removeByParameter[P <: Parameter[_]](c: ParameterSetType[T], parameterIn: P): T = {
    val f: Option[P] = getByParameter(c.paramSet, parameterIn)
    f match {
      case Some(parameter) => create(c.paramSet.-(parameter))
      case None            => c.asInstanceOf[T]
    }
  }

  // Function to find a parameter by keyname
  private def getByKeyname[P](parametersIn: Set[Parameter[_]], keyname: String): Option[P] =
    parametersIn.find(_.keyName == keyname).asInstanceOf[Option[P]]

  // Function to find a given parameter in the parameter set
  private def getByParameter[P](parametersIn: Set[Parameter[_]], parameter: Parameter[_]): Option[P] =
    parametersIn.find(_.equals(parameter)).asInstanceOf[Option[P]]

  /**
   * Method called by subclass to create a copy with the same key (or other fields) and new parameters. It is protected and
   * sublclasses should also keep it as protected to avoid receiving duplicate keys.
   */
  protected def create(data: Set[Parameter[_]]): T

  protected def dataToString: String = paramSet.mkString("(", ", ", ")")

  override def toString: String = s"$typeName$dataToString"

  /**
   * Returns true if the data contains the given key
   */
  def contains(key: Key[_]): Boolean = paramSet.exists(_.keyName == key.keyName)

  /**
   * Returns a set containing the names of any of the given keys that are missing in the data
   *
   * @param keys one or more keys
   */
  def missingKeys(keys: Key[_]*): Set[String] = {
    val argKeySet        = keys.map(_.keyName).toSet
    val parametersKeySet = paramSet.map(_.keyName)
    argKeySet.diff(parametersKeySet)
  }

  /**
   * java API: Returns a set containing the names of any of the given keys that are missing in the data
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
  def jGetStringMap: util.Map[String, String] = getStringMap.asJava

}
