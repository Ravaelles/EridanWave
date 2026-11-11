package Micro.Actions.Combat.Decisioning

import Micro.Actions.Combat.Decisioning.Kiting.CorsairSafety
import org.scalatest.funsuite.AnyFunSuite

class CorsairSafetySuite extends AnyFunSuite {

  test("a fresh Corsair keeps the base margin") {
    assert(CorsairSafety.avoidanceMarginTiles(0.0) == 1.8)
  }

  test("a fully wounded Corsair keeps the maximum margin") {
    assert(CorsairSafety.avoidanceMarginTiles(1.0) == 6.8)
  }

  test("the wound bonus applies linearly") {
    assert(CorsairSafety.avoidanceMarginTiles(0.5) == 4.3)
  }

  test("injury outside 0-1 clamps to the band edges") {
    assert(CorsairSafety.avoidanceMarginTiles(-0.5) == 1.8)
    assert(CorsairSafety.avoidanceMarginTiles(2.0) == 6.8)
  }
}
