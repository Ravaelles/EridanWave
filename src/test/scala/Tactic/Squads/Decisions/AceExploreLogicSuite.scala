package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class AceExploreLogicSuite extends AnyFunSuite {

  private val stale     = Seq("Tau", "Circuit", "LaMancha")
  private val fallback  = Seq("EnemyNat", "EnemyThird")

  test("a latch showing enemies holds while the fight continues") {
    assert(AceExploreLogic.pick(
      Some("Tau"), currentScouted = true, currentHasEnemies = true,
      stale, fallback, roll = 1).contains("Tau"))
  }

  test("an unscouted latch holds while still en route") {
    assert(AceExploreLogic.pick(
      Some("Tau"), currentScouted = false, currentHasEnemies = false,
      stale, fallback, roll = 1).contains("Tau"))
  }

  test("a scouted empty latch falls back to the likeliest enemy base") {
    assert(AceExploreLogic.pick(
      Some("Tau"), currentScouted = true, currentHasEnemies = false,
      stale, fallback, roll = 0).contains("EnemyNat"))
  }

  test("a scouted empty latch with no fallback takes a random stale base") {
    assert(AceExploreLogic.pick(
      Some("Tau"), currentScouted = true, currentHasEnemies = false,
      stale, fallback = Seq.empty, roll = 1).contains("Circuit"))
  }

  test("a negative roll still indexes a stale base") {
    assert(AceExploreLogic.pick(
      None, currentScouted = false, currentHasEnemies = false,
      stale, fallback, roll = -1).contains("LaMancha"))
  }

  test("no latch takes a random stale base") {
    assert(AceExploreLogic.pick(
      None, currentScouted = false, currentHasEnemies = false,
      stale, fallback, roll = 2).contains("LaMancha"))
  }

  test("no latch and no stale bases falls back to the likeliest enemy base") {
    assert(AceExploreLogic.pick(
      None, currentScouted = false, currentHasEnemies = false,
      Seq.empty, fallback, roll = 0).contains("EnemyNat"))
  }

  test("nothing anywhere stays without a base") {
    assert(AceExploreLogic.pick(
      None, currentScouted = false, currentHasEnemies = false,
      Seq.empty, Seq.empty, roll = 0).isEmpty)
  }
}
