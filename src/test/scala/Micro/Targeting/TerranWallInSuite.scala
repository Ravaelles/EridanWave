package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite
import testkit.TestKit

class TerranWallInSuite extends AnyFunSuite {

  private def walled(
    depotNear: Boolean = true,
    raxNear: Boolean = true,
    pathExists: Boolean = false): Boolean =
    TerranWallIn.shouldTreatAsWall(depotNear, raxNear, pathExists)

  test("a depot plus a rax with no path around is a sealed wall") {
    assert(walled())
  }

  test("one half alone is a building, not a wall") {
    assert( ! walled(depotNear = false))
    assert( ! walled(raxNear = false))
  }

  test("a leaky pile still lets the army through") {
    assert( ! walled(pathExists = true))
  }

  test("two tiles is in, past it is out") {
    assert(TerranWallIn.nearChoke(TerranWallIn.chokeRadiusTiles))
    assert( ! TerranWallIn.nearChoke(TerranWallIn.chokeRadiusTiles + 0.1))
  }
}
