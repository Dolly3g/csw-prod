package csw.param

import csw.param.Events.{ObserveEvent, StatusEvent, SystemEvent}
import csw.param.JsonSupport._
import csw.param.Parameters.{CommandInfo, Observe, Setup, Wait}
import csw.param.StateVariable.{CurrentState, DemandState}
import csw.param.parameters._
import csw.param.parameters.arrays._
import csw.param.parameters.matrices._
import csw.param.parameters.primitives._
import org.scalatest.FunSpec
import spray.json._

//noinspection ScalaUnusedSymbol
class JSONTests extends FunSpec {

  private val s1: String = "encoder"
  private val s2: String = "filter"
  private val s3: String = "detectorTemp"

  private val ck = "wfos.blue.filter"

  private val commandInfo = CommandInfo(ObsId("Obs001"))

  describe("Test Subsystem JSON") {
    val wfos: Subsystem = Subsystem.WFOS

    it("should encode and decode properly") {
      val json = wfos.toJson
      //info("wfos: " + json)
      val sub = json.convertTo[Subsystem]
      assert(sub == wfos)
    }
  }

  describe("Test concrete items") {

    it("char item encode/decode") {
      val k1 = Keys.CharKey.make(s3)
      val i1 = k1.set('d').withUnits(UnitsOfMeasure.NoUnits)

      val j1  = i1.toJson
      val in1 = j1.convertTo[GParam[Char]]
      assert(in1 == i1)
    }

    it("short item encode/decode") {
      val k1       = Keys.ShortKey.make(s3)
      val s: Short = -1
      val i1       = k1.set(s).withUnits(UnitsOfMeasure.NoUnits)

      val j1  = i1.toJson
      val in1 = j1.convertTo[GParam[Short]]
      assert(in1 == i1)
    }

    it("int item encode/decode") {
      val k1 = Keys.IntKey.make(s3)
      val i1 = k1.set(23).withUnits(UnitsOfMeasure.NoUnits)

      val j1  = i1.toJson
      val in1 = j1.convertTo[GParam[Int]]
      assert(in1 == i1)
    }

    it("long item encode/decode") {
      val k1 = LongKey(s1)
      val i1 = k1.set(123456L).withUnits(UnitsOfMeasure.NoUnits)

      val j1  = i1.toJson
      val in1 = j1.convertTo[LongParameter]
      assert(in1 == i1)
    }

    it("float item encode/decode") {
      val k1 = Keys.FloatKey.make(s1)
      val i1 = k1.set(123.456f).withUnits(UnitsOfMeasure.NoUnits)

      val j1  = i1.toJson
      val in1 = j1.convertTo[GParam[Float]]
      assert(in1 == i1)
    }

    it("double item encode/decode") {
      val k1 = Keys.DoubleKey.make(s1)
      val i1 = k1.set(123.456).withUnits(UnitsOfMeasure.NoUnits)

      val j1  = i1.toJson
      val in1 = j1.convertTo[GParam[Double]]
      assert(in1 == i1)
    }

    it("boolean item encode/decode") {
      val k1 = Keys.BooleanKey.make(s1)
      val i1 = k1.set(true, false).withUnits(UnitsOfMeasure.NoUnits)

      val j1 = i1.toJson
      //      info("j1: " + j1)
      val in1: GParam[Boolean] = j1.convertTo[GParam[Boolean]]
      assert(in1 == i1)

      val i2 = k1.set(true)

      val j2  = i2.toJson
      val in2 = j2.convertTo[GParam[Boolean]]
      assert(in2 == i2)
    }

    it("string item encode/decode") {
      val k1 = Keys.StringKey.make(s2)
      val i1 = k1.set("Blue", "Green").withUnits(UnitsOfMeasure.NoUnits)

      val j1  = i1.toJson
      val in1 = j1.convertTo[GParam[String]]
      assert(in1 == i1)
    }
  }

  describe("Testing Items") {

    val k1 = Keys.IntKey.make(s1)
    val k2 = Keys.StringKey.make(s2)

    val i1 = k1.set(22, 33, 44)
    val i2 = k2.set("a", "b", "c").withUnits(UnitsOfMeasure.degrees)

    it("should encode and decode items list") {
      // Use this to get a list to test
      val sc1   = Setup(commandInfo, ck).add(i1).add(i2)
      val items = sc1.paramSet

      val js3 = JsonSupport.paramSetFormat.write(items)
      val in1 = JsonSupport.paramSetFormat.read(js3)
      assert(in1 == items)
    }
  }

  describe("Setup JSON") {

    val k1 = Keys.CharKey.make("a")
    val k2 = Keys.IntKey.make("b")
    val k3 = LongKey("c")
    val k4 = Keys.FloatKey.make("d")
    val k5 = Keys.DoubleKey.make("e")
    val k6 = Keys.BooleanKey.make("f")
    val k7 = Keys.StringKey.make("g")

    val i1 = k1.set('d').withUnits(UnitsOfMeasure.NoUnits)
    val i2 = k2.set(22).withUnits(UnitsOfMeasure.NoUnits)
    val i3 = k3.set(1234L).withUnits(UnitsOfMeasure.NoUnits)
    val i4 = k4.set(123.45f).withUnits(UnitsOfMeasure.degrees)
    val i5 = k5.set(123.456).withUnits(UnitsOfMeasure.meters)
    val i6 = k6.set(false)
    val i7 = k7.set("GG495").withUnits(UnitsOfMeasure.degrees)

    it("Should encode/decode a Setup") {
      val c1 = Setup(commandInfo, ck).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = JsonSupport.writeSequenceCommand(c1)
      val c1in  = JsonSupport.readSequenceCommand[Setup](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
    }

    it("Should encode/decode an Observe") {
      val c1 = Observe(commandInfo, ck).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = JsonSupport.writeSequenceCommand(c1)
      val c1in  = JsonSupport.readSequenceCommand[Observe](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
    }

    it("Should encode/decode an StatusEvent") {
      val e1 = StatusEvent(ck).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(e1.size == 7)
      val e1out = JsonSupport.writeEvent(e1)
      val e1in  = JsonSupport.readEvent[StatusEvent](e1out)
      assert(e1in(k3).head == 1234L)
      assert(e1in.info.eventTime == e1.info.eventTime)
      assert(e1in == e1)
    }

    it("Should encode/decode an ObserveEvent") {
      val e1 = ObserveEvent(ck).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(e1.size == 7)
      val e1out = JsonSupport.writeEvent(e1)
      val e1in  = JsonSupport.readEvent[ObserveEvent](e1out)
      assert(e1in(k3).head == 1234L)
      assert(e1in == e1)
    }

    it("Should encode/decode an SystemEvent") {
      val e1 = SystemEvent(ck).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(e1.size == 7)
      val e1out = JsonSupport.writeEvent(e1)
      val e1in  = JsonSupport.readEvent[SystemEvent](e1out)
      assert(e1in(k3).head == 1234L)
      assert(e1in == e1)
    }

    it("Should encode/decode an CurrentState") {
      val c1 = CurrentState(ck).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = JsonSupport.writeStateVariable(c1)
      val c1in  = JsonSupport.readStateVariable[CurrentState](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
    }

    it("Should encode/decode an DemandState") {
      val c1 = DemandState(ck).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = JsonSupport.writeStateVariable(c1)
      val c1in  = JsonSupport.readStateVariable[DemandState](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
    }

    it("Should encode/decode an Wait") {
      val c1 = Wait(commandInfo, ck).add(i1).add(i2).add(i3).add(i4).add(i5).add(i6).add(i7)
      assert(c1.size == 7)
      val c1out = JsonSupport.writeSequenceCommand(c1)
      val c1in  = JsonSupport.readSequenceCommand[Wait](c1out)
      assert(c1in(k3).head == 1234L)
      assert(c1in == c1)
    }
  }

  describe("Test Custom RaDecItem") {
    it("Should allow custom RaDecItem") {
      val k1  = Keys.RaDecKey.make("coords")
      val c1  = RaDec(7.3, 12.1)
      val c2  = RaDec(9.1, 2.9)
      val i1  = k1.set(c1, c2)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1.get(k1).get.values.size == 2)
      assert(sc1.get(k1).get.values(0) == c1)
      assert(sc1.get(k1).get.values(1) == c2)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //        info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in.get(k1).get.values.size == 2)
      assert(sc1in.get(k1).get.values(0) == c1)
      assert(sc1in.get(k1).get.values(1) == c2)

      val sc2 = Setup(commandInfo, ck).add(k1.set(c1, c2))
      assert(sc2 == sc1)
    }
  }

  describe("Test Double Matrix items") {
    it("Should allow double matrix values") {
      val k1 = DoubleMatrixKey("myMatrix")
      val m1 = DoubleMatrix(
        Array(
          Array(1.0, 2.0, 3.0),
          Array(4.1, 5.1, 6.1),
          Array(7.2, 8.2, 9.2)
        )
      )
      val sc1 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Double Array items") {
    it("Should allow double array values") {
      val k1  = DoubleArrayKey("myArray")
      val m1  = DoubleArray(Array(1.0, 2.0, 3.0))
      val i1  = k1.set(m1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Int Matrix items") {
    it("Should allow int matrix values") {
      val k1  = IntMatrixKey("myMatrix")
      val m1  = IntMatrix(Array(Array(1, 2, 3), Array(4, 5, 6), Array(7, 8, 9)))
      val i1  = k1.set(m1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Int Array items") {
    it("Should allow int array values") {
      val k1  = Keys.IntArrayKey.make("myArray")
      val m1  = GArray(Array(1, 2, 3))
      val i1  = k1.set(m1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Byte Matrix items") {
    it("Should allow byte matrix values") {
      val k1  = ByteMatrixKey("myMatrix")
      val m1  = ByteMatrix(Array(Array[Byte](1, 2, 3), Array[Byte](4, 5, 6), Array[Byte](7, 8, 9)))
      val i1  = k1.set(m1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Byte Array items") {
    it("Should allow byte array values") {
      val k1  = ByteArrayKey("myArray")
      val m1  = ByteArray(Array[Byte](1, 2, 3))
      val i1  = k1.set(m1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Short Matrix items") {
    it("Should allow short matrix values") {
      val k1  = ShortMatrixKey("myMatrix")
      val m1  = ShortMatrix(Array.ofDim[Short](3, 3))
      val i1  = k1.set(m1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)

      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Short Array items") {
    it("Should allow short array values") {
      val k1  = ShortArrayKey("myArray")
      val m1  = ShortArray(Array[Short](1, 2, 3))
      val i1  = k1.set(m1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Long Matrix items") {
    it("Should allow long matrix values") {
      val k1  = LongMatrixKey("myMatrix")
      val m1  = LongMatrix(Array(Array(1, 2, 3), Array(4, 5, 6), Array(7, 8, 9)))
      val i1  = k1.set(m1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Long Array items") {
    it("Should allow long array values") {
      val k1  = LongArrayKey("myArray")
      val m1  = LongArray(Array(1, 2, 3))
      val i1  = k1.set(m1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == m1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == m1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(m1))
      assert(sc2 == sc1)
    }
  }

  describe("Test Choice items") {
    it("Should allow choice/enum values") {
      val k1  = ChoiceKey("myChoice", Choices.from("A", "B", "C"))
      val c1  = Choice("B")
      val i1  = k1.set(c1)
      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == c1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)
      //      info("sc1out: " + sc1out.prettyPrint)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == c1)

      val sc2 = Setup(commandInfo, ck).add(k1.set(c1))
      assert(sc2 == sc1)
    }
  }

  /*describe("testing StructItem JSON support") {
    it("should allow Struct values") {
      val k1 = StructKey("myStruct")

      val ra    = StringKey("ra")
      val dec   = StringKey("dec")
      val epoch = DoubleKey("epoch")
      val c1    = Struct().madd(ra.gset("12:13:14.1"), dec.gset("32:33:34.4"), epoch.gset(1950.0))
      val c2    = Struct().madd(ra.gset("12:13:15.2"), dec.gset("32:33:35.5"), epoch.gset(1950.0))

      val i1: StructParameter = k1.gset(c1, c2)

      val sc1 = Setup(commandInfo, ck).add(i1)
      assert(sc1(k1).head == c1)
      assert(sc1(k1).value(1) == c2)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)

      val s = sc1out.prettyPrint
      println(s) // XXX

      val sc1in: Parameters.Setup = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))
      assert(sc1in(k1).head == c1)
      //      val x = sc1in.get(k1).flatMap(_.head.get(ra))
      assert(sc1in(k1).head.get(ra).head.head == "12:13:14.1")

      //assert(sc1in(k1).value(1).name == "probe2")

      val sc2 = Setup(commandInfo, ck).add(k1.gset(c1))
      assert(sc2 == sc1)
    }
  }
 */
}
