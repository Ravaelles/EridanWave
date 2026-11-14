package Micro.Corsair

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class SpreadOutAirUnitsSuite extends AnyFunSuite {

  test("wings inside five tiles separate, outside hold") {
    assert(SpreadOutAirUnits.shouldSpread(newerId = 2, olderId = 1, distTiles = 0.0))
    assert(SpreadOutAirUnits.shouldSpread(newerId = 2, olderId = 1, distTiles = 4.99))
    assert( ! SpreadOutAirUnits.shouldSpread(newerId = 2, olderId = 1, distTiles = 5.0))
    assert( ! SpreadOutAirUnits.shouldSpread(newerId = 1, olderId = 2, distTiles = 0.0))
  }

  test("only the nearest older wing counts") {
    val others = Seq(
      SpreadOutAirUnits.OlderWing(5, Pixel(0, 0), 9.0),
      SpreadOutAirUnits.OlderWing(3, Pixel(0, 0), 2.0))
    assert(SpreadOutAirUnits.partner(7, others).map(_.id).contains(3))
    assert(SpreadOutAirUnits.partner(4, others).map(_.id).contains(3))
    assert(SpreadOutAirUnits.partner(2, others).isEmpty)
  }

  test("separation holds the full five tiles along the ray") {
    val away = SpreadOutAirUnits.awayFrom(Pixel(320, 320), Pixel(320, 0))
    assert(away.x == 320 && away.y == 5 * 32)
  }
}
