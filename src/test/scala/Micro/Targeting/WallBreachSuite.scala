package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class WallBreachSuite extends AnyFunSuite {

  test("a reachable goal never breaches, however close the building") {
    assert( ! WallBreach.breaches(noGroundPath = false, 0.0))
    assert( ! WallBreach.breaches(noGroundPath = false, 6.0))
  }

  test("a distant building is not the wall") {
    assert( ! WallBreach.breaches(noGroundPath = true, 6.01))
    assert( ! WallBreach.breaches(noGroundPath = true, 200.0))
  }

  test("a blocked goal with a building at arm's length breaches") {
    assert(WallBreach.breaches(noGroundPath = true, 0.0))
    assert(WallBreach.breaches(noGroundPath = true, 6.0))
  }
}
