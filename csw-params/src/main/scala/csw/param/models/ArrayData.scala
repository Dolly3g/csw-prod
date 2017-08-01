package csw.param.models

import csw.param.formats.WrappedArrayProtocol
import spray.json.{DefaultJsonProtocol, JsonFormat}

import scala.collection.mutable
import scala.language.implicitConversions
import scala.reflect.ClassTag

case class ArrayData[T](data: mutable.WrappedArray[T])

object ArrayData extends WrappedArrayProtocol with DefaultJsonProtocol {
  implicit def format[T: JsonFormat: ClassTag]: JsonFormat[ArrayData[T]] =
    jsonFormat1((xs: mutable.WrappedArray[T]) ⇒ new ArrayData[T](xs))

  implicit def fromArray[T](xs: Array[T]): ArrayData[T] = new ArrayData(xs)

  def fromArray[T: ClassTag](xs: T*): ArrayData[T] = new ArrayData(xs.toArray[T])
}
