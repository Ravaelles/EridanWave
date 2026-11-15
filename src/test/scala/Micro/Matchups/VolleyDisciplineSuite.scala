package Micro.Matchups

import org.scalatest.funsuite.AnyFunSuite

class VolleyDisciplineSuite extends AnyFunSuite {

  test("a slow cooldown volleys on matching reach") {
    assert(VolleyDiscipline.wants(
      cooldownMaxAgainstTarget = 30,
      potshotFrames = 15,
      rangeVsTarget = 200.0,
      rangeDeepestVsMe = 200.0,
      rangeNearestVsMe = 200.0,
      speed = 1.0,
      speedDeepest = 2.0))
  }

  test("a fast cooldown needs reach and speed against the deepest threat") {
    // Outranged and outrun: no volley.
    assert( ! VolleyDiscipline.wants(10, 15, 100.0, 200.0, 200.0, 1.0, 2.0))
    // Longer reach and faster legs: volley.
    assert(VolleyDiscipline.wants(10, 15, 200.0, 100.0, 100.0, 2.0, 1.0))
    // Longer reach but slower legs: the DPS axis vetoes.
    assert( ! VolleyDiscipline.wants(10, 15, 200.0, 100.0, 100.0, 1.0, 2.0))
  }

  test("the gap axis accepts the nearest threat's reach") {
    assert(VolleyDiscipline.wants(30, 15, 150.0, 200.0, 100.0, 1.0, 2.0))
    assert( ! VolleyDiscipline.wants(30, 15, 150.0, 200.0, 200.0, 1.0, 2.0))
  }
}
