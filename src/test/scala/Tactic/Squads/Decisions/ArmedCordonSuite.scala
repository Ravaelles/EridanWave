package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class ArmedCordonSuite extends AnyFunSuite {

  test("visible guns inside nineteen tiles hold the lone march") {
    assert(ArmedCordon.clearOfArmed(unitId = 101, nearestTiles = 10.0) == false)
    assert(ArmedCordon.clearOfArmed(unitId = 102, nearestTiles = 18.9) == false)
  }

  test("distant guns let the lone march through") {
    assert(ArmedCordon.clearOfArmed(unitId = 103, nearestTiles = 30.0) == true)
    assert(ArmedCordon.clearOfArmed(unitId = 104, nearestTiles = 21.1) == true)
  }

  test("the cordon holds its side inside the deadband") {
    assert(ArmedCordon.clearOfArmed(unitId = 105, nearestTiles = 30.0) == true)
    assert(ArmedCordon.clearOfArmed(unitId = 105, nearestTiles = 20.0) == true)
    assert(ArmedCordon.clearOfArmed(unitId = 105, nearestTiles = 18.9) == false)
    assert(ArmedCordon.clearOfArmed(unitId = 105, nearestTiles = 20.0) == false)
    assert(ArmedCordon.clearOfArmed(unitId = 105, nearestTiles = 21.1) == true)
  }
}
