package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class AirFleeGeometrySuite extends AnyFunSuite {

  test("fresh hulls keep the 2.5 base margin") {
    assert(AirFleeGeometry.safeMarginTiles(0.0) == 2.5)
  }

  test("wounds add up to 8 tiles linearly") {
    assert(AirFleeGeometry.safeMarginTiles(0.5) == 6.5)
    assert(AirFleeGeometry.safeMarginTiles(1.0) == 10.5)
  }

  test("no threats means no focus") {
    assert(AirFleeGeometry.focus(Seq.empty).isEmpty)
  }

  test("one threat focuses it straight") {
    val foe = Pixel(100, 100)
    assert(AirFleeGeometry.focus(Seq(foe)).contains(foe))
  }

  test("many threats focus their centroid") {
    val focus = AirFleeGeometry.focus(Seq(Pixel(0, 0), Pixel(320, 0)))
    assert(focus.exists(p => p.x == 160 && p.y == 0))
  }

  test("flee pixel runs from the focus past the margin") {
    val away = AirFleeGeometry.fleePixel(Pixel(320, 320), Pixel(320, 0), woundPercent = 0.0)
    assert(away.y > 320 && away.x == 320)
  }
}
