package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class RetreatForbiddenSuite extends AnyFunSuite {

  test("one comparison owns the choke: on it, past it, or four tiles out") {
    assert(RetreatForbidden.chokeMarginTiles == 4.0)
    assert(RetreatForbidden.duelRadiusTiles == 15.0)
    // On the choke line itself.
    assert(RetreatForbidden.forbidden(enemyIsProtossOrZerg = true, foeMainTiles = 12.0, homeRadiusTiles = 12.0, ourDistToFoeTiles = 15.0))
    // Past it toward the main.
    assert(RetreatForbidden.forbidden(enemyIsProtossOrZerg = true, foeMainTiles = 8.0, homeRadiusTiles = 12.0, ourDistToFoeTiles = 15.0))
    // Four tiles out still holds - the early stand.
    assert(RetreatForbidden.forbidden(enemyIsProtossOrZerg = true, foeMainTiles = 16.0, homeRadiusTiles = 12.0, ourDistToFoeTiles = 15.0))
  }

  test("other matchups, distant foes and far hulls keep the old vote") {
    assert( ! RetreatForbidden.forbidden(enemyIsProtossOrZerg = false, foeMainTiles = 8.0, homeRadiusTiles = 12.0, ourDistToFoeTiles = 0.0))
    assert( ! RetreatForbidden.forbidden(enemyIsProtossOrZerg = true, foeMainTiles = 16.1, homeRadiusTiles = 12.0, ourDistToFoeTiles = 0.0))
    assert( ! RetreatForbidden.forbidden(enemyIsProtossOrZerg = true, foeMainTiles = 8.0, homeRadiusTiles = 12.0, ourDistToFoeTiles = 15.1))
  }

  test("the latch absorbs mill noise instead of flapping the verdict") {
    assert( ! RetreatForbidden.released(foeMainTiles = 16.0, homeRadiusTiles = 12.0, ourDistToFoeTiles = 15.0))
    assert(RetreatForbidden.released(foeMainTiles = 17.1, homeRadiusTiles = 12.0, ourDistToFoeTiles = 15.0))
    assert(RetreatForbidden.released(foeMainTiles = 16.0, homeRadiusTiles = 12.0, ourDistToFoeTiles = 17.1))
  }

  test("a pack at home holds the natural against any race") {
    assert(RetreatForbidden.minDefenders == 7)
    assert(RetreatForbidden.defendNatural(ourWarriors = 7, foeNaturalTiles = 12.0, homeRadiusTiles = 12.0))
    assert(RetreatForbidden.defendNatural(ourWarriors = 30, foeNaturalTiles = 0.0, homeRadiusTiles = 12.0))
    assert( ! RetreatForbidden.defendNatural(ourWarriors = 6, foeNaturalTiles = 12.0, homeRadiusTiles = 12.0))
    assert( ! RetreatForbidden.defendNatural(ourWarriors = 7, foeNaturalTiles = 12.01, homeRadiusTiles = 12.0))
    assert(RetreatForbidden.releasedNatural(foeNaturalTiles = 13.1, homeRadiusTiles = 12.0))
    assert( ! RetreatForbidden.releasedNatural(foeNaturalTiles = 13.0, homeRadiusTiles = 12.0))
  }
}
