package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class MarchDressSuite extends AnyFunSuite {

  test("leader ahead of the pack mean waits") {
    assert(MarchDress.shouldWait(ownDistToGoalTiles = 8.0, meanDistToGoalTiles = 11.0))
  }

  test("units within tolerance of the mean march on") {
    assert( ! MarchDress.shouldWait(ownDistToGoalTiles = 9.0, meanDistToGoalTiles = 11.0))
    assert( ! MarchDress.shouldWait(ownDistToGoalTiles = 11.0, meanDistToGoalTiles = 11.0))
  }

  test("laggards behind the mean march on") {
    assert( ! MarchDress.shouldWait(ownDistToGoalTiles = 14.0, meanDistToGoalTiles = 11.0))
  }

  test("boundary is strict: exactly at tolerance marches, past it waits") {
    assert( ! MarchDress.shouldWait(ownDistToGoalTiles = 9.0, meanDistToGoalTiles = 9.0 + MarchDress.waitAheadTiles))
    assert(MarchDress.shouldWait(ownDistToGoalTiles = 9.0 - 0.01, meanDistToGoalTiles = 9.0 + MarchDress.waitAheadTiles))
  }
}
