package Micro.Actions.Combat.Spells.Protoss

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class SuicidePsionicSuite extends AnyFunSuite {

  test("a full battery with a chewed hull under fresh fire trades") {
    assert(SuicidePsionic.shouldSuicide(energy = 75, hitPoints = 44, framesSinceAttacked = 10))
    assert(SuicidePsionic.shouldSuicide(energy = 200, hitPoints = 1, framesSinceAttacked = 0))
  }

  test("a dry battery, a healthy hull or stale damage holds") {
    assert( ! SuicidePsionic.shouldSuicide(energy = 74, hitPoints = 44, framesSinceAttacked = 10))
    assert( ! SuicidePsionic.shouldSuicide(energy = 75, hitPoints = 45, framesSinceAttacked = 10))
    assert( ! SuicidePsionic.shouldSuicide(energy = 75, hitPoints = 44, framesSinceAttacked = 11))
  }

  test("the median centers the pack however it surrounds") {
    assert(SuicidePsionic.medianTarget(Seq.empty).isEmpty)
    assert(SuicidePsionic.medianTarget(Seq(Pixel(100, 200))).contains(Pixel(100, 200)))
    assert(SuicidePsionic.medianTarget(Seq(Pixel(0, 0), Pixel(100, 0), Pixel(0, 100), Pixel(100, 100)))
      .contains(Pixel(100, 100)))
    assert(SuicidePsionic.medianTarget(Seq(Pixel(0, 0), Pixel(60, 0), Pixel(1000, 1000)))
      .contains(Pixel(60, 0)))
  }
}
