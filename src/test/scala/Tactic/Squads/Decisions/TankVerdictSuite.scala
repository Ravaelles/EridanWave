package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class TankVerdictSuite extends AnyFunSuite {

  private def call(
    distTiles: Double,
    reachTiles: Double = 12.0,
    minTiles: Double = 2.0,
    hugNow: Boolean = false,
    localUs: Double = 100.0,
    localFoe: Double = 100.0,
    globalUs: Double = 100.0,
    globalFoe: Double = 100.0,
    fightingNow: Boolean = false): Option[TankVerdict.Call] =
    TankVerdict.desire(distTiles, reachTiles, minTiles, hugNow, localUs, localFoe, globalUs, globalFoe, fightingNow)

  test("inside the dead zone the guns shoot for free") {
    assert(call(distTiles = 1.0, localUs = 50.0, localFoe = 100.0) == Some(TankVerdict.Fight))
    assert(call(distTiles = 0.0, localUs = 0.0, localFoe = 100.0) == Some(TankVerdict.Fight))
  }

  test("a cooling hug walks the tank down instead of leaving it") {
    assert(call(distTiles = 6.0, hugNow = true, localUs = 50.0, localFoe = 100.0) == Some(TankVerdict.Fight))
  }

  test("past the standoff band no verdict applies") {
    assert(call(distTiles = 14.01, localUs = 300.0, localFoe = 100.0) == None)
    assert(call(distTiles = 20.0, localUs = 50.0, localFoe = 100.0) == None)
  }

  test("a superior force commits, an inferior one leaves") {
    assert(call(distTiles = 10.0, localUs = 200.0, localFoe = 100.0, globalUs = 200.0, globalFoe = 100.0) == Some(TankVerdict.Fight))
    assert(call(distTiles = 10.0, localUs = 199.0, localFoe = 100.0, globalUs = 200.0, globalFoe = 100.0) == Some(TankVerdict.Leave))
    assert(call(distTiles = 5.0, localUs = 50.0, localFoe = 100.0, globalUs = 50.0, globalFoe = 100.0) == Some(TankVerdict.Leave))
  }

  test("both fronts must agree: a losing war vetoes a winning fight") {
    assert(call(distTiles = 10.0, localUs = 300.0, localFoe = 100.0, globalUs = 100.0, globalFoe = 100.0) == Some(TankVerdict.Leave))
    assert(call(distTiles = 10.0, localUs = 100.0, localFoe = 100.0, globalUs = 300.0, globalFoe = 100.0) == Some(TankVerdict.Leave))
  }

  test("the commit holds through a dip, the leave needs a real edge") {
    assert(call(distTiles = 10.0, localUs = 150.0, localFoe = 100.0, globalUs = 150.0, globalFoe = 100.0, fightingNow = true) == Some(TankVerdict.Fight))
    assert(call(distTiles = 10.0, localUs = 149.0, localFoe = 100.0, globalUs = 200.0, globalFoe = 100.0, fightingNow = true) == Some(TankVerdict.Leave))
    assert(call(distTiles = 10.0, localUs = 150.0, localFoe = 100.0, globalUs = 150.0, globalFoe = 100.0, fightingNow = false) == Some(TankVerdict.Leave))
  }

  test("no enemy means no fear") {
    assert(call(distTiles = 10.0, localUs = 0.0, localFoe = 0.0, globalUs = 0.0, globalFoe = 0.0) == Some(TankVerdict.Fight))
    assert(call(distTiles = 10.0, localUs = 100.0, localFoe = 0.0, globalUs = 100.0, globalFoe = 0.0) == Some(TankVerdict.Fight))
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

  test("units do not share a verdict, reset clears it") {
    TankVerdict.reset()
    TankVerdict.update(7, TankVerdict.Leave)
    assert(TankVerdict.held(8) == None)
    TankVerdict.reset()
    assert(TankVerdict.held(7) == None)
  }
}
