package Macro.Scheduling.Simulation

import org.scalatest.funsuite.AnyFunSuite
import ProxyBwapi.Races.Protoss

class MacroStateAdvanceSuite extends AnyFunSuite {

  private def event(f: MacroEvent => Unit): MacroEvent = {
    val e = new MacroEvent(new MacroState)
    f(e)
    e
  }

  test("an event carries resources forward with passive income and caps supply") {
    val last = new MacroState
    last.minerals = 100
    last.gas = 50
    last.supplyAvailable = 380
    last.supplyUsed = 40
    last.mineralPatches = 8
    last.geysers = 2
    val next = new MacroState
    MacroStateAdvance.advance(
      last,
      next,
      event(e => {
        e.dMinerals = -50
        e.dGas = -50
        e.dSupplyAvailable = 30
        e.dSupplyUsed = 4
        e.dMineralPatches = -1
        e.dGeysers = 1
      }),
      dFrames = 10,
      mineralsPerFrame = 1.0,
      gasPerFrame = 0.5)

    assert(next.minerals == 100 - 50 + 10)
    assert(next.gas == 50 - 50 + 5)
    assert(next.supplyAvailable == 400) // Capped at the game's hard maximum
    assert(next.supplyUsed == 44)
    assert(next.mineralPatches == 7)
    assert(next.geysers == 3)
  }

  test("collections stay shared with the previous state when the event ignores them") {
    val last = new MacroState
    last.unitsExtant(Protoss.Zealot) = 2
    val next = new MacroState
    MacroStateAdvance.advance(last, next, event(_.dMineralPatches = 1), 0, 0.0, 0.0)

    assert(next.unitsExtant eq last.unitsExtant)
    assert(next.unitsComplete eq last.unitsComplete)
    assert(next.producers eq last.producers)
    assert(next.upgrades eq last.upgrades)
  }

  test("a touched collection is cloned, so the previous state is not rewritten") {
    val last = new MacroState
    last.unitsExtant(Protoss.Zealot) = 2
    val next = new MacroState
    MacroStateAdvance.advance(
      last,
      next,
      event(e => {
        e.dUnitExtant1 = Protoss.Zealot
        e.dUnitExtant1N = 1
      }),
      0,
      0.0,
      0.0)

    assert(next.unitsExtant(Protoss.Zealot) == 3)
    assert(last.unitsExtant(Protoss.Zealot) == 2)
    assert(next.unitsComplete eq last.unitsComplete)
  }

  test("techs and upgrades accumulate on top of the previous state") {
    val last = new MacroState
    last.upgrades(Protoss.ZealotSpeed) = 2
    val next = new MacroState
    MacroStateAdvance.advance(
      last,
      next,
      event(e => {
        e.dTech = Protoss.Hallucination
        e.dUpgrade = Protoss.ZealotSpeed
        e.dUpgradeLevel = 3
        e.dUnitComplete = Protoss.Dragoon
        e.dUnitCompleteN = 1
      }),
      0,
      0.0,
      0.0)

    assert(next.techs.contains(Protoss.Hallucination))
    assert( ! last.techs.contains(Protoss.Hallucination))
    assert(next.upgrades(Protoss.ZealotSpeed) == 3)
    assert(last.upgrades(Protoss.ZealotSpeed) == 2)
    assert(next.unitsComplete(Protoss.Dragoon) == 1)
  }

  test("an addon delta is charged to producers like any other producer") {
    val next = new MacroState
    MacroStateAdvance.advance(
      new MacroState,
      next,
      event(e => {
        e.dAddon = Protoss.Zealot // Any class will do: the delta is just applied to producers
        e.dAddonN = 1
      }),
      0,
      0.0,
      0.0)

    assert(next.producers(Protoss.Zealot) == 1)
  }
}
