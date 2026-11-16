package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class BreakTerranWallInSuite extends AnyFunSuite {

  private def breaks(
    shieldWound: Int = 0,
    seesTerranBuilding: Boolean = true,
    framesSinceAttack: Int = BreakTerranWallIn.quietFrames + 1,
    enemyHasSiegedTank: Boolean = false,
    distTiles: Double = 0.0): Boolean =
    BreakTerranWallIn.shouldBreak(shieldWound, seesTerranBuilding, framesSinceAttack, enemyHasSiegedTank, distTiles)

  test("a fresh quiet column breaks a choke wall with no siege") {
    assert(breaks())
  }

  test("a chewed column retreats instead of knocking") {
    assert( ! breaks(shieldWound = BreakTerranWallIn.maxShieldWound + 1))
    assert(breaks(shieldWound = BreakTerranWallIn.maxShieldWound))
  }

  test("recent shots mean a duel is already on") {
    assert( ! breaks(framesSinceAttack = BreakTerranWallIn.quietFrames))
    assert(breaks(framesSinceAttack = BreakTerranWallIn.quietFrames + 1))
  }

  test("sieged tanks outrank masonry") {
    assert( ! breaks(enemyHasSiegedTank = true))
  }

  test("a base five tiles off the lane is not the wall") {
    assert( ! breaks(distTiles = BreakTerranWallIn.chokeRadiusTiles + 0.1))
    assert(breaks(distTiles = BreakTerranWallIn.chokeRadiusTiles))
  }

  test("no visible wall means nothing to chew") {
    assert( ! breaks(seesTerranBuilding = false))
  }
}
