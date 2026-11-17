package Micro.Actions.Combat.Maneuvering

import org.scalatest.funsuite.AnyFunSuite

class DOdgeSuite extends AnyFunSuite {

  test("far from home always dodges") {
    assert(DOdge.shouldDodge(distMainTiles = 40.0, homeRadiusTiles = 20.0, cooldownLeft = 0))
    assert(DOdge.shouldDodge(distMainTiles = 20.0, homeRadiusTiles = 20.0, cooldownLeft = 0))
  }

  test("at home only a cooling gun dodges") {
    assert(DOdge.shouldDodge(distMainTiles = 10.0, homeRadiusTiles = 20.0, cooldownLeft = 6))
    assert(DOdge.shouldDodge(distMainTiles = 10.0, homeRadiusTiles = 20.0, cooldownLeft = 60))
    assert( ! DOdge.shouldDodge(distMainTiles = 10.0, homeRadiusTiles = 20.0, cooldownLeft = 5))
    assert( ! DOdge.shouldDodge(distMainTiles = 10.0, homeRadiusTiles = 20.0, cooldownLeft = 0))
  }

  test("unknown home radius never holds the dodge") {
    assert(DOdge.shouldDodge(distMainTiles = 10.0, homeRadiusTiles = 0.0, cooldownLeft = 0))
  }
}
