package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class RetreatForbiddenSuite extends AnyFunSuite {

  test("an enemy on or past the choke pins our duelists") {
    assert(RetreatForbidden.chokeRadiusTiles == 4.0)
    assert(RetreatForbidden.duelRadiusTiles == 15.0)
    // On the choke and dueling: hold.
    assert(RetreatForbidden.forbidden(enemyIsProtossOrZerg = true, foeChokeTiles = 4.0, foePastChoke = false, ourDistToFoeTiles = 15.0))
    // Past the choke toward main: hold even from farther out.
    assert(RetreatForbidden.forbidden(enemyIsProtossOrZerg = true, foeChokeTiles = 99.0, foePastChoke = true, ourDistToFoeTiles = 15.0))
    assert(RetreatForbidden.forbidden(enemyIsProtossOrZerg = true, foeChokeTiles = 0.0, foePastChoke = false, ourDistToFoeTiles = 0.0))
  }

  test("other matchups, far foes and stragglers retreat as before") {
    // Terran keeps its own doctrine.
    assert( ! RetreatForbidden.forbidden(enemyIsProtossOrZerg = false, foeChokeTiles = 0.0, foePastChoke = true, ourDistToFoeTiles = 0.0))
    // Nobody near the choke and nobody past it: march as before.
    assert( ! RetreatForbidden.forbidden(enemyIsProtossOrZerg = true, foeChokeTiles = 4.01, foePastChoke = false, ourDistToFoeTiles = 0.0))
    // Too far from the foe to matter: a straggler rejoins instead.
    assert( ! RetreatForbidden.forbidden(enemyIsProtossOrZerg = true, foeChokeTiles = 0.0, foePastChoke = false, ourDistToFoeTiles = 15.01))
  }
}
