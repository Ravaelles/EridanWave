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

  private def breached(
    unitToPieceTiles: Double = 3.0,
    framesSinceAttack: Int = TerranWallIn.quietSealedFrames + 1,
    pieceToChokeTiles: Double = 3.0,
    sealedWall: Boolean = true): Boolean =
    TerranWallIn.shouldBreakSealed(unitToPieceTiles, framesSinceAttack, pieceToChokeTiles, sealedWall)

  test("the user rule: close, quiet past three seconds, sealed") {
    assert(breached())
  }

  test("a swinging gun is a duel, not a wall") {
    assert( ! breached(framesSinceAttack = TerranWallIn.quietSealedFrames))
  }

  test("an unsealed lane walks around, never chews") {
    assert( ! breached(sealedWall = false))
  }

  test("far hulls and far pieces stay out") {
    assert( ! breached(unitToPieceTiles = TerranWallIn.sealedBreachTiles + 0.1))
    assert( ! breached(pieceToChokeTiles = TerranWallIn.sealedBreachTiles + 0.1))
  }
}
