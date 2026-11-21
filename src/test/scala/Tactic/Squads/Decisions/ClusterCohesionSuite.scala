package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class ClusterCohesionSuite extends AnyFunSuite {

  private def rejoin(
    warrior: Boolean = true,
    target: Boolean = false,
    retreating: Boolean = false,
    cooling: Boolean = false,
    battle: Boolean = true,
    macroGame: Boolean = true,
    contact: Boolean = false,
    verdictDuel: Boolean = false,
    dist: Double = 20.0): Boolean =
    ClusterCohesion.shouldRejoin(warrior, target, retreating, cooling, battle, macroGame, contact, verdictDuel, dist)

  test("a lost warrior rejoins past fourteen tiles") {
    assert(ClusterCohesion.maxDriftTiles == 14.0)
    assert(rejoin())
    assert( ! rejoin(dist = 14.0))
  }

  test("fighting, retreating and cooling hulls keep their business") {
    assert( ! rejoin(target = true))
    assert( ! rejoin(retreating = true))
    assert( ! rejoin(cooling = true))
    assert( ! rejoin(contact = true))
  }

  test("marches, raids en route and baseless maps keep their rules") {
    assert( ! rejoin(battle = false))
    assert( ! rejoin(macroGame = false))
    assert( ! rejoin(warrior = false))
  }

  test("a latched duel pins the hull: no rejoin without a target pick") {
    assert( ! rejoin(verdictDuel = true))
    assert( ! rejoin(target = true, verdictDuel = true))
  }
}
