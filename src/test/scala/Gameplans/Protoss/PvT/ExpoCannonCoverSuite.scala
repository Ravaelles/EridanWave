package Gameplans.Protoss.PvT

import org.scalatest.funsuite.AnyFunSuite

class ExpoCannonCoverSuite extends AnyFunSuite {

  private def covers(
    enemyIsTerran: Boolean = true,
    armyStrengthUnits: Double = ExpoCannonCover.weakArmyUnits): Boolean =
    ExpoCannonCover.shouldCover(enemyIsTerran, armyStrengthUnits)

  test("a weak army covers every expo against Terran") {
    assert(covers())
  }

  test("a strong army needs no cover") {
    assert( ! covers(armyStrengthUnits = ExpoCannonCover.weakArmyUnits + 1))
    assert(covers(armyStrengthUnits = ExpoCannonCover.weakArmyUnits))
  }

  test("other races hold their own doctrine") {
    assert( ! covers(enemyIsTerran = false))
  }

  test("four vultures double the expo cover") {
    assert(ExpoCannonCover.shouldCoverVsVultures(enemyIsTerran = true, vultures = 4))
    assert(ExpoCannonCover.shouldCoverVsVultures(enemyIsTerran = true, vultures = 12))
    assert( ! ExpoCannonCover.shouldCoverVsVultures(enemyIsTerran = true, vultures = 3))
    assert( ! ExpoCannonCover.shouldCoverVsVultures(enemyIsTerran = false, vultures = 9))
    assert(ExpoCannonCover.vultureCannonsPerExpo == 2)
  }
}
