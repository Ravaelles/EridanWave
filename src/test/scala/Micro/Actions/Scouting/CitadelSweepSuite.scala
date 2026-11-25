package Micro.Actions.Scouting

import org.scalatest.funsuite.AnyFunSuite

class CitadelSweepSuite extends AnyFunSuite {

  private def allTrue: Boolean =
    CitadelSweep.fires(
      pvp = true, umsMode = false, isScoutProbe = true, inEnemyMetro = true,
      coreKnown = true, answered = false, unscoutedBases = true)

  test("the sweep fires only with every entry condition up") {
    assert(allTrue)
  }

  test("no sweep without a plausible Citadel") {
    assert( ! CitadelSweep.fires(
      pvp = true, umsMode = false, isScoutProbe = true, inEnemyMetro = true,
      coreKnown = false, answered = false, unscoutedBases = true))
  }

  test("no sweep once the Citadel question is answered") {
    assert( ! CitadelSweep.fires(
      pvp = true, umsMode = false, isScoutProbe = true, inEnemyMetro = true,
      coreKnown = true, answered = true, unscoutedBases = true))
  }

  test("no sweep with the base already swept") {
    assert( ! CitadelSweep.fires(
      pvp = true, umsMode = false, isScoutProbe = true, inEnemyMetro = true,
      coreKnown = true, answered = false, unscoutedBases = false))
  }

  test("no sweep outside the enemy base") {
    assert( ! CitadelSweep.fires(
      pvp = true, umsMode = false, isScoutProbe = true, inEnemyMetro = false,
      coreKnown = true, answered = false, unscoutedBases = true))
  }

  test("ladder PvP only: no UMS, no other matchup, no non-scout") {
    assert( ! CitadelSweep.fires(
      pvp = true, umsMode = true, isScoutProbe = true, inEnemyMetro = true,
      coreKnown = true, answered = false, unscoutedBases = true))
    assert( ! CitadelSweep.fires(
      pvp = false, umsMode = false, isScoutProbe = true, inEnemyMetro = true,
      coreKnown = true, answered = false, unscoutedBases = true))
    assert( ! CitadelSweep.fires(
      pvp = true, umsMode = false, isScoutProbe = false, inEnemyMetro = true,
      coreKnown = true, answered = false, unscoutedBases = true))
  }

  test("the mission releases on answered, swept, walked out or reassigned") {
    assert(CitadelSweep.releases(answered = true, swept = false, leftMetro = false, isScout = true))
    assert(CitadelSweep.releases(answered = false, swept = true, leftMetro = false, isScout = true))
    assert(CitadelSweep.releases(answered = false, swept = false, leftMetro = true, isScout = true))
    assert(CitadelSweep.releases(answered = false, swept = false, leftMetro = false, isScout = false))
  }

  test("the mission holds while nothing releases it") {
    assert( ! CitadelSweep.releases(answered = false, swept = false, leftMetro = false, isScout = true))
  }
}
