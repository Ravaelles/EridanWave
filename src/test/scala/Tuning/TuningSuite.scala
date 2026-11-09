package Tuning

import _root_.Tuning.Strategies.TestStrategy
import org.scalatest.funsuite.AnyFunSuite

class TuningSuite extends AnyFunSuite {

  private val random = new java.util.Random(42)

  test("explicit override is used verbatim") {
    assert(Tuning.resolveValue(Some("1.5"), 0.0, 10.0, random) == ((1.5, true)))
  }

  test("absent key randomizes uniformly within bounds") {
    val (value, fromOverride) = Tuning.resolveValue(None, 3.0, 7.0, random)
    assert( ! fromOverride)
    assert(value >= 3.0 && value <= 7.0)
  }

  test("garbage override falls back to random within bounds") {
    val (value, fromOverride) = Tuning.resolveValue(Some("abc"), 3.0, 7.0, random)
    assert( ! fromOverride)
    assert(value >= 3.0 && value <= 7.0)
  }

  test("integer values round for display") {
    assert(Tuning.formatValue(1.6, isInt = true) == "2")
    assert(Tuning.formatValue(1.5, isInt = false) == "1.5")
  }

  test("strategies resolve by short name") {
    assert(Tuning.resolve("TestStrategy").name == "Test")
  }

  test("unknown strategy names fail loudly") {
    assertThrows[IllegalArgumentException] { Tuning.resolve("NopeMissing") }
  }

  test("vector digest is deterministic and content-sensitive") {
    val a = Tuning.vectorDigest(Map("x" -> "1", "y" -> "2"))
    assert(a == Tuning.vectorDigest(Map("x" -> "1", "y" -> "2")))
    assert(a.length == 32)
    assert(a != Tuning.vectorDigest(Map("x" -> "1", "y" -> "3")))
  }

  test("applyParamsTo assigns overrides and remembers the vector") {
    val strategy = new TestStrategy
    val key = "eridanwave.tune.Test.knob"
    System.setProperty(key, "1.25")
    try {
      Tuning.applyParamsTo(strategy, random)
      assert(strategy.knob == 1.25)
      assert(Tuning.appliedParams("Test.knob") == "1.25")
    } finally {
      System.clearProperty(key)
    }
  }
}
