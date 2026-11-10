package Tactic.Missions.evaluation

import org.scalatest.funsuite.AnyFunSuite

class MissionHarassSuite extends AnyFunSuite {

  test("workers inside eight tiles are denied") {
    assert(MissionHarass.denying(0.0))
    assert(MissionHarass.denying(7.9))
  }

  test("workers at eight tiles mine on") {
    assert( ! MissionHarass.denying(8.0))
    assert( ! MissionHarass.denying(20.0))
  }
}
