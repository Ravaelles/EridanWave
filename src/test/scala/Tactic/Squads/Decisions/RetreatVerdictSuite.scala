package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class RetreatVerdictSuite extends AnyFunSuite {

  private def hold(
    isWarrior: Boolean = true,
    isReaver: Boolean = false,
    canFlee: Boolean = true,
    stormHold: Boolean = false,
    shouldFight: Boolean = false,
    totalHp: Int = 200,
    unitsScore: Double = 100.0,
    nearestArmedTiles: Double = 40.0,
    proxyRush: Boolean = false): Boolean =
    RetreatVerdict.shouldHold(
      isWarrior,
      isReaver,
      canFlee,
      stormHold,
      shouldFight,
      totalHp,
      unitsScore,
      nearestArmedTiles,
      proxyRush)

  test("a beaten warrior with room to run holds the retreat") {
    assert(hold())
  }

  test("winning the verdict releases the retreat") {
    assert( ! hold(shouldFight = true))
  }

  test("non-warriors never hold a warrior retreat") {
    assert( ! hold(isWarrior = false))
  }

  test("reavers keep their own brain") {
    assert( ! hold(isReaver = true))
  }

  test("a committed hull holds no retreat (sides with the technique arm)") {
    assert( ! hold(canFlee = false))
  }

  test("a casting templar holds its storm instead") {
    assert( ! hold(stormHold = true))
  }

  test("retreat permission still gates the verdict") {
    assert( ! hold(unitsScore = 866.0, proxyRush = false))
    assert(hold(unitsScore = 866.0, nearestArmedTiles = 3.0))
    assert( ! hold(unitsScore = 100.0, proxyRush = true))
  }

  test("a dying hull always keeps its way out") {
    assert(hold(totalHp = 35, unitsScore = 866.0, nearestArmedTiles = 40.0, proxyRush = true))
    assert( ! hold(totalHp = 36, unitsScore = 866.0, nearestArmedTiles = 40.0, proxyRush = true))
  }
}
