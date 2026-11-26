package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class CombatDistancesSuite extends AnyFunSuite {

  test("blades arrive with the pack fight, never at sight") {
    assert( ! CombatDistances.meleeArrivesWithPack(
      hasHittable = false, hasContact = false, squadEngagedUpon = false, squadEngagingOn = false))
  }

  test("its own duel always lets the blade charge") {
    assert(CombatDistances.meleeArrivesWithPack(
      hasHittable = true, hasContact = false, squadEngagedUpon = false, squadEngagingOn = false))
    assert(CombatDistances.meleeArrivesWithPack(
      hasHittable = false, hasContact = true, squadEngagedUpon = false, squadEngagingOn = false))
  }

  test("the pack fight releases every blade, under fire or dealing it") {
    assert(CombatDistances.meleeArrivesWithPack(
      hasHittable = false, hasContact = false, squadEngagedUpon = true, squadEngagingOn = false))
    assert(CombatDistances.meleeArrivesWithPack(
      hasHittable = false, hasContact = false, squadEngagedUpon = false, squadEngagingOn = true))
  }
}
