package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class TankVerdictSuite extends AnyFunSuite {

  private def call(
    distTiles: Double,
    reachTiles: Double = 12.0,
    minTiles: Double = 2.0,
    hugNow: Boolean = false,
    canDuel: Boolean = true,
    lonely: Boolean = false,
    usSkim: Double = 100.0,
    foeSkim: Double = 100.0,
    fightingNow: Boolean = false): Option[TankVerdict.Call] =
    TankVerdict.desire(distTiles, reachTiles, minTiles, hugNow, canDuel, lonely, usSkim, foeSkim, fightingNow)

  test("non-duelists abstain: the keep-off covers them instead") {
    assert(call(distTiles = 5.0, canDuel = false) == None)
    assert(call(distTiles = 13.0, canDuel = false) == None)
  }

  test("inside the dead zone the guns shoot for free") {
    assert(call(distTiles = 1.0, minTiles = 2.0) == Some(TankVerdict.Fight))
    assert(call(distTiles = 0.0, minTiles = 2.0, usSkim = 0.0, foeSkim = 100.0) == Some(TankVerdict.Fight))
  }

  test("a cooling hug walks the tank down instead of leaving it") {
    assert(call(distTiles = 6.0, hugNow = true, lonely = true, usSkim = 50.0, foeSkim = 100.0) == Some(TankVerdict.Fight))
    assert(call(distTiles = 6.0, hugNow = true, lonely = false, usSkim = 200.0, foeSkim = 100.0) == Some(TankVerdict.Fight))
  }

  test("a cooling hug never walks into covering guns") {
    assert(call(distTiles = 6.0, hugNow = true, lonely = false, usSkim = 50.0, foeSkim = 100.0) == Some(TankVerdict.Leave))
    assert(call(distTiles = 6.0, hugNow = true, lonely = false, usSkim = 150.0, foeSkim = 100.0, fightingNow = true) == Some(TankVerdict.Fight))
  }

  test("past the standoff band the duel has not started") {
    assert(call(distTiles = 14.01, usSkim = 300.0, foeSkim = 100.0) == None)
    assert(call(distTiles = 20.0, usSkim = 50.0, foeSkim = 100.0) == None)
  }

  test("a lonely tank is always walked down") {
    assert(call(distTiles = 10.0, lonely = true, usSkim = 50.0, foeSkim = 100.0) == Some(TankVerdict.Fight))
    assert(call(distTiles = 5.0, lonely = true, usSkim = 10.0, foeSkim = 100.0) == Some(TankVerdict.Fight))
  }

  test("an escorted tank takes two-to-one to commit") {
    assert(call(distTiles = 10.0, lonely = false, usSkim = 200.0, foeSkim = 100.0) == Some(TankVerdict.Fight))
    assert(call(distTiles = 10.0, lonely = false, usSkim = 199.0, foeSkim = 100.0) == Some(TankVerdict.Leave))
    assert(call(distTiles = 5.0, lonely = false, usSkim = 50.0, foeSkim = 100.0) == Some(TankVerdict.Leave))
  }

  test("the commit holds through a dip, the leave needs a real edge") {
    assert(call(distTiles = 10.0, lonely = false, usSkim = 150.0, foeSkim = 100.0, fightingNow = true) == Some(TankVerdict.Fight))
    assert(call(distTiles = 10.0, lonely = false, usSkim = 149.0, foeSkim = 100.0, fightingNow = true) == Some(TankVerdict.Leave))
    assert(call(distTiles = 10.0, lonely = false, usSkim = 150.0, foeSkim = 100.0, fightingNow = false) == Some(TankVerdict.Leave))
  }

  test("the owner persists past the band instead of renegotiating it") {
    assert(TankVerdict.owner(Some(TankVerdict.Fight), Some(TankVerdict.Leave)) == Some(TankVerdict.Leave))
    assert(TankVerdict.owner(Some(TankVerdict.Leave), Some(TankVerdict.Fight)) == Some(TankVerdict.Fight))
    assert(TankVerdict.owner(Some(TankVerdict.Fight), None) == Some(TankVerdict.Fight))
    assert(TankVerdict.owner(Some(TankVerdict.Leave), None) == Some(TankVerdict.Leave))
    assert(TankVerdict.owner(None, Some(TankVerdict.Leave)) == Some(TankVerdict.Leave))
    assert(TankVerdict.owner(None, None) == None)
  }

  test("an unknown unit holds no verdict") {
    TankVerdict.reset()
    assert(TankVerdict.held(7) == None)
  }

  test("the bus ticks desire and holds it") {
    TankVerdict.reset()
    TankVerdict.update(7, TankVerdict.Leave)
    assert(TankVerdict.held(7) == Some(TankVerdict.Leave))
    TankVerdict.update(7, TankVerdict.Fight)
    assert(TankVerdict.held(7) == Some(TankVerdict.Fight))
  }

  test("a cleared unit holds no verdict, and units do not share one") {
    TankVerdict.reset()
    TankVerdict.update(7, TankVerdict.Leave)
    TankVerdict.clear(7)
    assert(TankVerdict.held(7) == None)
    TankVerdict.update(7, TankVerdict.Leave)
    assert(TankVerdict.held(8) == None)
    TankVerdict.reset()
    assert(TankVerdict.held(7) == None)
  }
}
