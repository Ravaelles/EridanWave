package Micro.Agency

import org.scalatest.funsuite.AnyFunSuite

class ArrivalFlowSuite extends AnyFunSuite {

  test("flows past a waypoint short of a further final") {
    assert(ArrivalFlow.pushPast(distToTargetTiles = 1.0, distToFinalTiles = 10.0, aligned = true) == ArrivalFlow.overshootTiles)
  }

  test("true arrivals brake normally") {
    assert(ArrivalFlow.pushPast(distToTargetTiles = 1.0, distToFinalTiles = 1.0, aligned = true) == 0.0)
    assert(ArrivalFlow.pushPast(distToTargetTiles = 0.0, distToFinalTiles = 0.0, aligned = true) == 0.0)
  }

  test("distant waypoints need no push") {
    assert(ArrivalFlow.pushPast(distToTargetTiles = 5.0, distToFinalTiles = 20.0, aligned = true) == 0.0)
  }

  test("backsteps and sidesteps brake normally") {
    assert(ArrivalFlow.pushPast(distToTargetTiles = 1.0, distToFinalTiles = 10.0, aligned = false) == 0.0)
  }
}
