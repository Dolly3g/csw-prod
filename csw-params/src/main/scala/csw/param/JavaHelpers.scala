package csw.param

import csw.param.Parameters.ParameterSetType
import csw.param.UnitsOfMeasure.Units
import csw.param.parameters._
import csw.param.parameters.arrays._
import csw.param.parameters.matrices._
import csw.param.parameters.primitives._

import scala.annotation.varargs
import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._

/**
 * TMT Source Code: 6/23/16.
 */
private[param] object JavaHelpers {

  // ByteArrayItem
  def jvalue(item: ByteArrayParameter): ByteArray = item.values(0)

  def jvalue(item: ByteArrayParameter, index: Int): ByteArray = item.values(index)

  def jvalues(item: ByteArrayParameter): java.util.List[ByteArray] = item.values.asJava

  def jget(item: ByteArrayParameter, index: Int): java.util.Optional[ByteArray] = item.get(index).asJava

  def jset(key: ByteArrayKey, v: java.util.List[ByteArray], units: Units): ByteArrayParameter =
    ByteArrayParameter(key.keyName, v.asScala.toVector, units)

  @varargs
  def jset(key: ByteArrayKey, v: ByteArray*) =
    ByteArrayParameter(key.keyName, v.toVector, units = UnitsOfMeasure.NoUnits)

  // ByteMatrixItem
  def jvalue(item: ByteMatrixParameter): ByteMatrix = item.values(0)

  def jvalue(item: ByteMatrixParameter, index: Int): ByteMatrix = item.values(index)

  def jvalues(item: ByteMatrixParameter): java.util.List[ByteMatrix] = item.values.asJava

  def jget(item: ByteMatrixParameter, index: Int): java.util.Optional[ByteMatrix] = item.get(index).asJava

  def jset(key: ByteMatrixKey, v: java.util.List[ByteMatrix], units: Units): ByteMatrixParameter =
    ByteMatrixParameter(key.keyName, v.asScala.toVector, units)

  // DoubleArrayItem
  def jvalue(item: DoubleArrayParameter): DoubleArray = item.values(0)

  def jvalue(item: DoubleArrayParameter, index: Int): DoubleArray = item.values(index)

  def jvalues(item: DoubleArrayParameter): java.util.List[DoubleArray] = item.values.asJava

  def jget(item: DoubleArrayParameter, index: Int): java.util.Optional[DoubleArray] = item.get(index).asJava

  def jset(key: DoubleArrayKey, v: java.util.List[DoubleArray], units: Units): DoubleArrayParameter =
    DoubleArrayParameter(key.keyName, v.asScala.toVector, units)

  @varargs
  def jset(key: DoubleArrayKey, v: DoubleArray*) =
    DoubleArrayParameter(key.keyName, v.toVector, units = UnitsOfMeasure.NoUnits)

  // DoubleMatrixItem
  def jvalue(item: DoubleMatrixParameter): DoubleMatrix = item.values(0)

  def jvalue(item: DoubleMatrixParameter, index: Int): DoubleMatrix = item.values(index)

  def jvalues(item: DoubleMatrixParameter): java.util.List[DoubleMatrix] = item.values.asJava

  def jget(item: DoubleMatrixParameter, index: Int): java.util.Optional[DoubleMatrix] = item.get(index).asJava

  def jset(key: DoubleMatrixKey, v: java.util.List[DoubleMatrix], units: Units): DoubleMatrixParameter =
    DoubleMatrixParameter(key.keyName, v.asScala.toVector, units)

  @varargs
  def jset(key: DoubleMatrixKey, v: DoubleMatrix*) =
    DoubleMatrixParameter(key.keyName, v.toVector, units = UnitsOfMeasure.NoUnits)

  // FloatItem
  def jvalue(item: FloatParameter): java.lang.Float = item.values(0)

  def jvalue(item: FloatParameter, index: Int): java.lang.Float = item.values(index)

  def jvalues(item: FloatParameter): java.util.List[java.lang.Float] = item.values.map(i => i: java.lang.Float).asJava

  def jget(item: FloatParameter, index: Int): java.util.Optional[java.lang.Float] =
    item.get(index).map(i => i: java.lang.Float).asJava

  def jset(key: FloatKey, v: java.util.List[java.lang.Float], units: Units): FloatParameter =
    FloatParameter(key.keyName, v.asScala.toVector.map(i => i: Float), units)

  @varargs
  def jset(key: FloatKey, v: java.lang.Float*) =
    FloatParameter(key.keyName, v.map(i => i: Float).toVector, units = UnitsOfMeasure.NoUnits)

  // FloatArrayItem
  def jvalue(item: FloatArrayParameter): FloatArray = item.values(0)

  def jvalue(item: FloatArrayParameter, index: Int): FloatArray = item.values(index)

  def jvalues(item: FloatArrayParameter): java.util.List[FloatArray] = item.values.asJava

  def jget(item: FloatArrayParameter, index: Int): java.util.Optional[FloatArray] = item.get(index).asJava

  def jset(key: FloatArrayKey, v: java.util.List[FloatArray], units: Units): FloatArrayParameter =
    FloatArrayParameter(key.keyName, v.asScala.toVector, units)

  @varargs
  def jset(key: FloatArrayKey, v: FloatArray*) =
    FloatArrayParameter(key.keyName, v.toVector, units = UnitsOfMeasure.NoUnits)

  // FloatMatrixItem
  def jvalue(item: FloatMatrixParameter): FloatMatrix = item.values(0)

  def jvalue(item: FloatMatrixParameter, index: Int): FloatMatrix = item.values(index)

  def jvalues(item: FloatMatrixParameter): java.util.List[FloatMatrix] = item.values.asJava

  def jget(item: FloatMatrixParameter, index: Int): java.util.Optional[FloatMatrix] = item.get(index).asJava

  def jset(key: FloatMatrixKey, v: java.util.List[FloatMatrix], units: Units): FloatMatrixParameter =
    FloatMatrixParameter(key.keyName, v.asScala.toVector, units)

  @varargs
  def jset(key: FloatMatrixKey, v: FloatMatrix*) =
    FloatMatrixParameter(key.keyName, v.toVector, units = UnitsOfMeasure.NoUnits)

  // LongItem
  def jvalue(item: LongParameter): java.lang.Long = item.values(0)

  def jvalue(item: LongParameter, index: Int): java.lang.Long = item.values(index)

  def jvalues(item: LongParameter): java.util.List[java.lang.Long] = item.values.map(i => i: java.lang.Long).asJava

  def jget(item: LongParameter, index: Int): java.util.Optional[java.lang.Long] =
    item.get(index).map(i => i: java.lang.Long).asJava

  def jset(key: LongKey, v: java.util.List[java.lang.Long], units: Units): LongParameter =
    LongParameter(key.keyName, v.asScala.toVector.map(i => i: Long), units)

  @varargs
  def jset(key: LongKey, v: java.lang.Long*) =
    LongParameter(key.keyName, v.map(i => i: Long).toVector, units = UnitsOfMeasure.NoUnits)

  // LongArrayItem
  def jvalue(item: LongArrayParameter): LongArray = item.values(0)

  def jvalue(item: LongArrayParameter, index: Int): LongArray = item.values(index)

  def jvalues(item: LongArrayParameter): java.util.List[LongArray] = item.values.asJava

  def jget(item: LongArrayParameter, index: Int): java.util.Optional[LongArray] = item.get(index).asJava

  def jset(key: LongArrayKey, v: java.util.List[LongArray], units: Units): LongArrayParameter =
    LongArrayParameter(key.keyName, v.asScala.toVector, units)

  @varargs
  def jset(key: LongArrayKey, v: LongArray*) =
    LongArrayParameter(key.keyName, v.toVector, units = UnitsOfMeasure.NoUnits)

  // LongMatrixItem
  def jvalue(item: LongMatrixParameter): LongMatrix = item.values(0)

  def jvalue(item: LongMatrixParameter, index: Int): LongMatrix = item.values(index)

  def jvalues(item: LongMatrixParameter): java.util.List[LongMatrix] = item.values.asJava

  def jget(item: LongMatrixParameter, index: Int): java.util.Optional[LongMatrix] = item.get(index).asJava

  def jset(key: LongMatrixKey, v: java.util.List[LongMatrix], units: Units): LongMatrixParameter =
    LongMatrixParameter(key.keyName, v.asScala.toVector, units)

  @varargs
  def jset(key: LongMatrixKey, v: LongMatrix*) =
    LongMatrixParameter(key.keyName, v.toVector, units = UnitsOfMeasure.NoUnits)

  // ShortArrayItem
  def jvalue(item: ShortArrayParameter): ShortArray = item.values(0)

  def jvalue(item: ShortArrayParameter, index: Int): ShortArray = item.values(index)

  def jvalues(item: ShortArrayParameter): java.util.List[ShortArray] = item.values.asJava

  def jget(item: ShortArrayParameter, index: Int): java.util.Optional[ShortArray] = item.get(index).asJava

  def jset(key: ShortArrayKey, v: java.util.List[ShortArray], units: Units): ShortArrayParameter =
    ShortArrayParameter(key.keyName, v.asScala.toVector, units)

  @varargs
  def jset(key: ShortArrayKey, v: ShortArray*) =
    ShortArrayParameter(key.keyName, v.toVector, units = UnitsOfMeasure.NoUnits)

  // ShortMatrixItem
  def jvalue(item: ShortMatrixParameter): ShortMatrix = item.values(0)

  def jvalue(item: ShortMatrixParameter, index: Int): ShortMatrix = item.values(index)

  def jvalues(item: ShortMatrixParameter): java.util.List[ShortMatrix] = item.values.asJava

  def jget(item: ShortMatrixParameter, index: Int): java.util.Optional[ShortMatrix] = item.get(index).asJava

  def jset(key: ShortMatrixKey, v: java.util.List[ShortMatrix], units: Units): ShortMatrixParameter =
    ShortMatrixParameter(key.keyName, v.asScala.toVector, units)

  @varargs
  def jset(key: ShortMatrixKey, v: ShortMatrix*) =
    ShortMatrixParameter(key.keyName, v.toVector, units = UnitsOfMeasure.NoUnits)

  // StringItem
  def jvalue(item: StringParameter): java.lang.String = item.values(0)

  def jvalue(item: StringParameter, index: Int): java.lang.String = item.values(index)

  def jvalues(item: StringParameter): java.util.List[java.lang.String] =
    item.values.map(i => i: java.lang.String).asJava

  def jget(item: StringParameter, index: Int): java.util.Optional[java.lang.String] =
    item.get(index).map(i => i: java.lang.String).asJava

  def jset(key: StringKey, v: java.util.List[java.lang.String], units: Units): StringParameter =
    StringParameter(key.keyName, v.asScala.toVector.map(i => i: String), units)

  @varargs
  def jset(key: StringKey, v: java.lang.String*) =
    StringParameter(key.keyName, v.map(i => i: String).toVector, units = UnitsOfMeasure.NoUnits)

  def jadd[I <: Parameter[_], T <: ParameterSetType[T]](sc: T, items: java.util.List[I]): T = {
    val x = items.asScala
    x.foldLeft(sc)((r, i) => r.add(i))
  }

  def jget[S, I <: Parameter[S], T <: ParameterSetType[T]](sc: T, key: Key[S, I]): java.util.Optional[I] =
    sc.get(key).asJava

  def jget[S, I <: Parameter[S], T <: ParameterSetType[T], J](sc: T,
                                                              key: Key[S, I],
                                                              index: Int): java.util.Optional[J] = {
    sc.get(key) match {
      case Some(item) =>
        (if (index >= 0 && index < item.size) Some(item.values(index).asInstanceOf[J]) else None).asJava
      case None => None.asJava
    }
  }

  def jvalue[S, I <: Parameter[S], T <: ParameterSetType[T], J](sc: T, key: Key[S, I]): J = {
    val item = sc.get(key)
    item match {
      case Some(x) => x.values(0).asInstanceOf[J]
      case None    => throw new NoSuchElementException(s"Item: $key not found")
    }
  }

  def jvalues[S, I <: Parameter[S], T <: ParameterSetType[T], J](sc: T, key: Key[S, I]): java.util.List[J] = {
    val item = sc.get(key)
    item match {
      case Some(x) => x.values.map(i => i.asInstanceOf[J]).asJava
      case None    => throw new NoSuchElementException(s"Item: $key not found")
    }
  }

  // ChoiceItem
  def jvalue(item: ChoiceParameter): Choice = item.values(0)

  def jvalue(item: ChoiceParameter, index: Int): Choice = item.values(index)

  def jvalues(item: ChoiceParameter): java.util.List[Choice] = item.values.map(i => i: Choice).asJava

  def jget(item: ChoiceParameter, index: Int): java.util.Optional[Choice] = item.get(index).map(i => i: Choice).asJava

  def jset(key: ChoiceKey, v: java.util.List[Choice], units: Units): ChoiceParameter =
    ChoiceParameter(key.keyName, key.choices, v.asScala.toVector.map(i => i: Choice), units)

  @varargs
  def jset(key: ChoiceKey, v: Choice*) =
    ChoiceParameter(key.keyName, key.choices, v.map(i => i: Choice).toVector, units = UnitsOfMeasure.NoUnits)

  // StructItem
  def jvalue(item: StructParameter): Struct = item.values(0)

  def jvalue(item: StructParameter, index: Int): Struct = item.values(index)

  def jvalues(item: StructParameter): java.util.List[Struct] = item.values.map(i => i: Struct).asJava

  def jget(item: StructParameter, index: Int): java.util.Optional[Struct] = item.get(index).map(i => i: Struct).asJava

  def jset(key: StructKey, v: java.util.List[Struct], units: Units): StructParameter =
    StructParameter(key.keyName, v.asScala.toVector.map(i => i: Struct), units)

  @varargs
  def jset(key: StructKey, v: Struct*) =
    StructParameter(key.keyName, v.map(i => i: Struct).toVector, units = UnitsOfMeasure.NoUnits)
}
