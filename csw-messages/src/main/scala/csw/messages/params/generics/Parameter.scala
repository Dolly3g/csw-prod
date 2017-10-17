package csw.messages.params.generics

import java.util
import java.util.Optional

import com.trueaccord.scalapb.TypeMapper
import csw.messages.TMTSerializable
import csw.messages.params.models.Units
import csw.messages.params.pb.{ItemType, ItemsFactory}
import csw_params.parameter.PbParameter
import csw_params.parameter.PbParameter.Items
import spray.json.{pimpAny, DefaultJsonProtocol, JsObject, JsValue, JsonFormat}

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.mutable
import scala.compat.java8.OptionConverters.RichOptionForJava8
import scala.reflect.ClassTag

object Parameter {

  import DefaultJsonProtocol._

  private[generics] def apply[S: JsonFormat: ClassTag: ItemsFactory](
      keyName: String,
      keyType: KeyType[S],
      items: mutable.WrappedArray[S],
      units: Units
  ): Parameter[S] =
    new Parameter(keyName, keyType, items, units)

  implicit def parameterFormat2: JsonFormat[Parameter[_]] = new JsonFormat[Parameter[_]] {
    override def write(obj: Parameter[_]): JsValue = obj.toJson

    override def read(json: JsValue): Parameter[_] = {
      val value = json.asJsObject.fields("keyType").convertTo[KeyType[_]]
      value.paramFormat.read(json)
    }
  }

  implicit def parameterFormat[T: JsonFormat: ClassTag: ItemsFactory]: JsonFormat[Parameter[T]] =
    new JsonFormat[Parameter[T]] {
      override def write(obj: Parameter[T]): JsValue = {
        JsObject(
          "keyName" -> obj.keyName.toJson,
          "keyType" -> obj.keyType.toJson,
          "values"  -> obj.values.toJson,
          "units"   -> obj.units.toJson
        )
      }

      override def read(json: JsValue): Parameter[T] = {
        val fields = json.asJsObject.fields
        Parameter(
          fields("keyName").convertTo[String],
          fields("keyType").convertTo[KeyType[T]],
          fields("values").convertTo[Array[T]],
          fields("units").convertTo[Units]
        )
      }
    }

  def apply[T](implicit x: JsonFormat[Parameter[T]]): JsonFormat[Parameter[T]] = x

  implicit def typeMapper[S: ClassTag: JsonFormat: ItemsFactory]: TypeMapper[PbParameter, Parameter[S]] =
    new TypeMapper[PbParameter, Parameter[S]] {
      override def toCustom(pbParameter: PbParameter): Parameter[S] = Parameter(
        pbParameter.name,
        pbParameter.keyType.asInstanceOf[KeyType[S]],
        cswItems(pbParameter.items),
        pbParameter.units
      )

      override def toBase(x: Parameter[S]): PbParameter =
        PbParameter()
          .withName(x.keyName)
          .withUnits(x.units)
          .withKeyType(x.keyType)
          .withItems(ItemsFactory[S].make(x.items))
    }

  implicit val typeMapper2: TypeMapper[PbParameter, Parameter[_]] =
    TypeMapper[PbParameter, Parameter[_]](p ⇒ p.keyType.typeMapper.toCustom(p))(p => p.toPb)

  def cswItems[T: ClassTag](items: Items): mutable.WrappedArray[T] = items.value match {
    case x: ItemType[_] ⇒ x.asInstanceOf[ItemType[T]].values.toArray[T]
    case x              ⇒ throw new RuntimeException(s"unexpected type ${x.getClass} found, ItemType expected")
  }

}

case class Parameter[S: JsonFormat: ClassTag: ItemsFactory] private[messages] (
    keyName: String,
    keyType: KeyType[S],
    items: mutable.WrappedArray[S],
    units: Units
) extends TMTSerializable {

  def values: Array[S] = items.array

  def jValues: util.List[S] = items.asJava

  /**
   * The number of values in this parameter (values.size)
   *
   * @return
   */
  def size: Int = items.size

  /**
   * Returns the value at the given index, throwing an exception if the index is out of range
   *
   * @param index the index of a value
   * @return the value at the given index (may throw an exception if the index is out of range)
   */
  def apply(index: Int): S = value(index)

  /**
   * Returns the value at the given index, throwing an exception if the index is out of range
   * This is a Scala convenience method
   *
   * @param index the index of a value
   * @return the value at the given index (may throw an exception if the index is out of range)
   */
  def value(index: Int): S = items(index)

  /**
   * @param index the index of a value
   * @return Some value at the given index as an Option, if the index is in range, otherwise None
   */
  def get(index: Int): Option[S]    = items.lift(index)
  def jGet(index: Int): Optional[S] = items.lift(index).asJava

  /**
   * Returns the first value as a convenience when storing a single value
   *
   * @return the first or default value (Use this if you know there is only a single value)
   */
  def head: S = value(0)

  /**
   * Sets the units for the values
   *
   * @param unitsIn the units for the values
   * @return a new instance of this parameter with the units set
   */
  def withUnits(unitsIn: Units): Parameter[S] = copy(units = unitsIn)

  def valuesToString: String = items.mkString("(", ",", ")")
  override def toString      = s"$keyName($valuesToString$units)"
  def toJson: JsValue        = Parameter[S].write(this)
  def toPb: PbParameter      = Parameter.typeMapper[S].toBase(this)
}
