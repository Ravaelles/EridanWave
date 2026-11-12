package Micro.Actions.Protoss.Shuttle

import org.scalatest.funsuite.AnyFunSuite

class ShuttleQuestCostSuite extends AnyFunSuite {

  test("a passenger in reach outranks a safe one by four orders of magnitude") {
    val safe = ShuttleQuestCost.finalCost(baseCost = 320, multiplier = 1.0, needsLift = false)
    val lift = ShuttleQuestCost.finalCost(baseCost = 320, multiplier = 1.0, needsLift = true)
    assert(safe == 320.0)
    assert(lift == 0.032)
    assert(lift < safe / 1000)
  }

  test("a passenger in the fight is not penalized while the others fight") {
    assert(ShuttleQuestCost.multiplier(passengerFighting = true, anyoneFighting = true) == 1.0)
  }

  test("only passengers left out of a fight wait ten times longer") {
    assert(ShuttleQuestCost.multiplier(passengerFighting = false, anyoneFighting = true) == 10.0)
  }

  test("with nobody fighting, every errand keeps its plain cost") {
    assert(ShuttleQuestCost.multiplier(passengerFighting = false, anyoneFighting = false) == 1.0)
  }

  test("a safe passenger never steals the shuttle from one that needs it") {
    val idle = ShuttleQuestCost.finalCost(baseCost = 64, multiplier = ShuttleQuestCost.multiplier(passengerFighting = false, anyoneFighting = true), needsLift = false)
    val lift = ShuttleQuestCost.finalCost(baseCost = 320, multiplier = ShuttleQuestCost.multiplier(passengerFighting = true, anyoneFighting = true), needsLift = true)
    assert(lift < idle)
  }

  test("a shooter waits out its drop cooldown before a pickup pays off") {
    assert(ShuttleQuestCost.pickupPatienceFrames(canAttack = true, cooldownOnDrop = 40) == 64)
  }

  test("a passenger that cannot shoot only waits the base travel time") {
    assert(ShuttleQuestCost.pickupPatienceFrames(canAttack = false, cooldownOnDrop = 40) == 24)
  }
}
