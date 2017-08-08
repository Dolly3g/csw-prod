package csw.param.generics

import csw.param.commands.{CommandInfo, Setup}
import csw.param.models.{ArrayData, MatrixData, ObsId}
import csw.param.generics.KeyType.LongMatrixKey
import csw.param.generics.ParameterSetDsl._
import csw.units.Units.{degrees, meters, NoUnits}
import org.scalatest.{FunSpec, Matchers}

/**
 * Tests the config DSL
 */
class ParameterSetDslTest extends FunSpec with Matchers {
  private val s1: String = "encoder"
  private val s2: String = "filter"
  private val s3: String = "detectorTemp"
  //  private val ck: String = "wfos.blue.filter"
  private val ck1: String = "wfos.prog.cloudcover"
  private val ck2: String = "wfos.red.filter"
  //  private val ck3: String = "wfos.red.detector"
  private val commandInfo = CommandInfo(ObsId("Obs001"))

  describe("creating items") {
    import csw.param.generics.ParameterSetDsl.{size ⇒ ssize}

    val k1           = KeyType.IntKey.make(s1)
    val detectorTemp = KeyType.DoubleKey.make(s3)

    // DEOPSCSW-190: Implement Unit Support
    it("should work to set single items") {
      val i1 = set(k1, 2)
      i1 shouldBe an[Parameter[Int]]
      ssize(i1) should equal(1)
      units(i1) should be(NoUnits)
    }

    it("should work to set multiple items") {
      val i1 = set(k1, 1, 2, 3, 4, 5)
      ssize(i1) should equal(5)
    }

    // DEOPSCSW-190: Implement Unit Support
    it("should work with units too") {
      val i1 = set(detectorTemp, 100.0).withUnits(degrees)
      i1 shouldBe an[Parameter[Double]]
      ssize(i1) shouldBe 1
      units(i1) should be(degrees)
    }
  }

  describe("checking simple values") {
    import csw.param.generics.ParameterSetDsl.{value ⇒ svalue}
    val k1 = KeyType.IntKey.make(s1)

    it("should have value access") {
      val i1 = set(k1, 1, 2, 3, 4, 5)
      i1.size should equal(5)

      values(i1) should equal(Vector(1, 2, 3, 4, 5))
      head(i1) should equal(1)
      svalue(i1, 0) should equal(1)
      svalue(i1, 1) should equal(2)
      svalue(i1, 2) should equal(3)
      svalue(i1, 3) should equal(4)
      svalue(i1, 4) should equal(5)

      intercept[IndexOutOfBoundsException] {
        svalue(i1, 5)
      }
    }
  }

  describe("work with an array type") {
    val k1 = KeyType.LongArrayKey.make(s2)

    it("should allow setting") {
      val m1: Array[Long] = Array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
      val m2: Array[Long] = Array(100, 1000, 10000)

      val i1 = k1.set(m1, m2)
      i1.size should be(2)
      head(i1) should equal(ArrayData(m1))
      head(i1).data should equal(m1)
      values(i1) should equal(Vector(ArrayData(m1), ArrayData(m2)))
      values(i1)(0) should equal(ArrayData(m1))
      values(i1)(1) should equal(ArrayData(m2))
    }
  }

  describe("work with a matrix type") {
    val k1 = LongMatrixKey.make(s2)

    it("should allow setting") {
      val m1: Array[Array[Long]] = Array(Array(1, 2, 4), Array(2, 4, 8), Array(4, 8, 16))
      // Note that LongMatrix implicit doesn't work here?
      val i1 = set(k1, MatrixData.fromArrays(m1))
      i1.size should be(1)
      head(i1) should equal(MatrixData.fromArrays(m1))
      head(i1).data should equal(m1)
      values(i1) should equal(Vector(MatrixData.fromArrays(m1)))
      values(i1)(0) should equal(MatrixData.fromArrays(m1))
    }
  }

  describe("checking optional get values") {
    val k1 = KeyType.IntKey.make(s1)

    it("should have value access") {
      val i1 = set(k1, 1, 2, 3, 4, 5)
      i1.size should equal(5)
      values(i1) should equal(Vector(1, 2, 3, 4, 5))

      get(i1, 0) should equal(Option(1))
      get(i1, 1) should equal(Option(2))
      // Out of range gives None
      get(i1, 9) should equal(None)
    }
  }

  describe("adding items to sc") {
    val k1 = KeyType.IntKey.make(s1)
    val k2 = KeyType.StringKey.make(s2)
    val k3 = KeyType.DoubleKey.make(s3)

    it("should allow adding single items") {
      val sc1 = add(Setup(commandInfo, ck1), set(k1, 1000))
      sc1.size should be(1)
    }

    it("shoudl allow adding several at once") {
      val sc2 = madd(Setup(commandInfo, ck2), set(k1, 1000), set(k2, "1000"), set(k3, 1000.0))

      sc2.size should be(3)
      exists(sc2, k1) shouldBe true
      exists(sc2, k2) shouldBe true
      exists(sc2, k3) shouldBe true
    }
  }

  describe("accessing items in an sc") {
    val k1 = KeyType.IntKey.make(s1)
    val k2 = KeyType.StringKey.make(s2)
    val k3 = KeyType.DoubleKey.make(s3)

    val i1 = set(k1, 1000)
    val i2 = set(k2, "1000")
    val i3 = set(k3, 1000.0)

    it("should allow accessing existing items") {
      val sc1 = madd(Setup(commandInfo, ck2), i1, i2, i3)
      sc1.size should be(3)

      parameter(sc1, k1) should equal(i1)
      parameter(sc1, k2) should equal(i2)
      parameter(sc1, k3) should equal(i3)
    }

    it("should throw NoSuchElementException if not present") {
      val sc1 = madd(Setup(commandInfo, ck2), i1, i2, i3)

      val k4 = KeyType.FloatKey.make("not present")

      exists(sc1, k1) shouldBe true
      exists(sc1, k2) shouldBe true
      exists(sc1, k3) shouldBe true
      exists(sc1, k4) shouldBe false

      intercept[NoSuchElementException] {
        parameter(sc1, k4)
      }
    }
  }

  describe("accessing items in an sc as option") {
    val k1 = KeyType.IntKey.make(s1)
    val k2 = KeyType.StringKey.make(s2)
    val k3 = KeyType.DoubleKey.make(s3)

    val i1 = set(k1, 1000)
    val i2 = set(k2, "1000")
    val i3 = set(k3, 1000.0)

    it("should allow accessing existing items") {
      var sc1 = Setup(commandInfo, ck2)
      sc1 = madd(sc1, i1, i2, i3)
      csize(sc1) should be(3)

      get(sc1, k1) should equal(Option(i1))
      get(sc1, k2) should equal(Option(i2))
      get(sc1, k3) should equal(Option(i3))
    }

    it("should be None if not present") {
      val sc1 = madd(Setup(commandInfo, ck2), i1, i2, i3)

      val k4 = KeyType.FloatKey.make("not present")
      get(sc1, k1) should equal(Option(i1))
      get(sc1, k2) should equal(Option(i2))
      get(sc1, k3) should equal(Option(i3))
      get(sc1, k4) should equal(None)
    }
  }

  describe("should allow option get") {
    val k1 = KeyType.IntKey.make(s1)
    val k2 = KeyType.StringKey.make(s2)
    val k3 = KeyType.DoubleKey.make(s3)
    val k4 = KeyType.StringKey.make("Not Present")

    val i1 = set(k1, 1000, 2000)
    val i2 = set(k2, "1000", "2000")
    val i3 = set(k3, 1000.0, 2000.0)

    it("should allow accessing existing items") {
      val sc1 = madd(Setup(commandInfo, ck2), i1, i2, i3)
      csize(sc1) should be(3)

      get(sc1, k1, 0) should be(Some(1000))
      get(sc1, k2, 1) should be(Some("2000"))
      // Out of range
      get(sc1, k3, 3) should be(None)
      // Non existent item
      get(sc1, k4, 0) should be(None)
    }
  }

  describe("removing items from a configuration by keyname") {
    val k1 = KeyType.IntKey.make("itest")
    val k2 = KeyType.DoubleKey.make("dtest")
    val k3 = KeyType.StringKey.make("stest")
    val k4 = KeyType.LongArrayKey.make("lartest")

    val i1 = set(k1, 1, 2, 3).withUnits(degrees)
    val i2 = set(k2, 1.0, 2.0, 3.0).withUnits(meters)
    val i3 = set(k3, "A", "B", "C")
    val i4 = set(k4, ArrayData(Array.fill[Long](100)(10)), ArrayData(Array.fill[Long](100)(100)))

    it("Should allow removing one at a time") {
      var sc1 = madd(Setup(commandInfo, ck1), i1, i2, i3, i4)
      csize(sc1) should be(4)
      get(sc1, k1).isDefined should be(true)
      get(sc1, k2).isDefined should be(true)
      get(sc1, k3).isDefined should be(true)
      get(sc1, k4).isDefined should be(true)

      sc1 = remove(sc1, k1)
      csize(sc1) should be(3)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(true)
      get(sc1, k3).isDefined should be(true)
      get(sc1, k4).isDefined should be(true)

      sc1 = remove(sc1, k2)
      csize(sc1) should be(2)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(true)
      get(sc1, k4).isDefined should be(true)

      sc1 = remove(sc1, k3)
      csize(sc1) should be(1)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(false)
      get(sc1, k4).isDefined should be(true)

      // Should allow removing non-existent
      sc1 = remove(sc1, k3)
      csize(sc1) should be(1)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(false)
      get(sc1, k4).isDefined should be(true)

      sc1 = remove(sc1, k4)
      csize(sc1) should be(0)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(false)
      get(sc1, k4).isDefined should be(false)

      // Add allows re-adding
      sc1 = add(sc1, i4)
      csize(sc1) should be(1)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(false)
      get(sc1, k4).isDefined should be(true)
    }
  }

  describe("removing items from a configuration as items") {
    val k1 = KeyType.IntKey.make("itest")
    val k2 = KeyType.DoubleKey.make("dtest")
    val k3 = KeyType.StringKey.make("stest")
    val k4 = KeyType.LongArrayKey.make("lartest")

    val i1 = set(k1, 1, 2, 3).withUnits(degrees)
    val i2 = set(k2, 1.0, 2.0, 3.0).withUnits(meters)
    val i3 = set(k3, "A", "B", "C")
    val i4 = set(k4, ArrayData(Array.fill[Long](100)(10)), ArrayData(Array.fill[Long](100)(100)))

    it("Should allow removing one at a time") {
      var sc1 = madd(Setup(commandInfo, ck1), i1, i2, i3, i4)
      sc1.size should be(4)
      get(sc1, k1).isDefined should be(true)
      get(sc1, k2).isDefined should be(true)
      get(sc1, k3).isDefined should be(true)
      get(sc1, k4).isDefined should be(true)

      sc1 = remove(sc1, i1)
      sc1.size should be(3)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(true)
      get(sc1, k3).isDefined should be(true)
      get(sc1, k4).isDefined should be(true)

      sc1 = remove(sc1, i2)
      sc1.size should be(2)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(true)
      get(sc1, k4).isDefined should be(true)

      sc1 = remove(sc1, i3)
      sc1.size should be(1)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(false)
      get(sc1, k4).isDefined should be(true)

      // Should allow removing non-existent
      sc1 = remove(sc1, i3)
      sc1.size should be(1)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(false)
      get(sc1, k4).isDefined should be(true)

      sc1 = remove(sc1, i4)
      sc1.size should be(0)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(false)
      get(sc1, k4).isDefined should be(false)

      // Add allows re-adding
      sc1 = add(sc1, i4)
      sc1.size should be(1)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(false)
      get(sc1, k4).isDefined should be(true)
    }
  }

  // DEOPSCSW-190: Implement Unit Support
  describe("sc tests") {
    val k1 = KeyType.IntKey.make("itest")
    val k2 = KeyType.DoubleKey.make("dtest")
    //    val k3 = StringKey("stest")
    //    val k4 = LongArrayKey("lartest")

    val i1 = set(k1, 1, 2, 3).withUnits(degrees)
    val i2 = set(k2, 1.0, 2.0, 3.0).withUnits(meters)

    it("should allow creation") {
      val sc1 = setup(commandInfo, ck2, i1, i2)
      csize(sc1) should be(2)
      exists(sc1, k1) shouldBe true
      exists(sc1, k2) shouldBe true

      val sc2 = setup(
        commandInfo,
        ck2,
        k1 -> 3 withUnits degrees,
        k2 -> 44.3 withUnits meters
      )
      csize(sc2) should be(2)
      exists(sc2, k1) shouldBe true
      exists(sc2, k2) shouldBe true
      units(parameter(sc2, k1)) shouldBe degrees
      units(parameter(sc2, k2)) shouldBe meters
    }
  }

  describe("config as template tests") {
    val zeroPoint = KeyType.IntKey.make("zeroPoint")
    val filter    = KeyType.StringKey.make("filter")
    val mode      = KeyType.StringKey.make("mode")

    val i1 = set(mode, "Fast")    // default value
    val i2 = set(filter, "home")  // default value
    val i3 = set(zeroPoint, 1000) // Where home is

    it("should create overriding defaults") {
      val default: Setup = setup(commandInfo, ck2, i1, i2, i3)

      val sc1 = add(default, zeroPoint -> 2000)

      val intItem = sc1(zeroPoint)

      csize(sc1) shouldBe 3
      parameter(sc1, zeroPoint) should equal(intItem)
      head(parameter(sc1, zeroPoint)) should equal(2000)
      parameter(sc1, filter) should equal(i2)
      parameter(sc1, mode) should equal(i1)

      // Check that default has not changed
      parameter(default, zeroPoint) should equal(i3)
      parameter(default, filter) should equal(i2)
      parameter(default, mode) should equal(i1)
    }
  }
}