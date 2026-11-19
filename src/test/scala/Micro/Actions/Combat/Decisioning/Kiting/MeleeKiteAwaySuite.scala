package Micro.Actions.Combat.Decisioning.Kiting

import org.scalatest.funsuite.AnyFunSuite

class MeleeKiteAwaySuite extends AnyFunSuite {

  private def retreat(
    totalHp: Int = 200,
    coolingDown: Boolean = true,
    hasSlowLingNearby: Boolean = false,
    outnumberedByZealots: Boolean = false,
    attackingMelee: Boolean = false): Boolean =
    MeleeKiteAway.shouldRetreat(totalHp, coolingDown, hasSlowLingNearby, outnumberedByZealots, attackingMelee)

  test("a ready gun never yields ground") {
    assert( ! retreat(coolingDown = false, hasSlowLingNearby = true))
    assert( ! retreat(coolingDown = false, outnumberedByZealots = true))
    assert( ! retreat(totalHp = 10, coolingDown = false, attackingMelee = true))
  }

  test("slow lings nearby break the line at any health") {
    assert(retreat(totalHp = 200, hasSlowLingNearby = true))
    assert(retreat(totalHp = 10, hasSlowLingNearby = true))
  }

  test("speedlings never break the line: too fast to kite, stand and swing") {
    // Speedlings arrive as hasSlowLingNearby = false (IsSlowling seam);
    // cooling with no other reason holds, at any health.
    assert( ! retreat(totalHp = 200, hasSlowLingNearby = false))
    assert( ! retreat(totalHp = 10, hasSlowLingNearby = false))
  }

  test("a superior zealot pack breaks the line at any health") {
    assert(retreat(totalHp = 200, outnumberedByZealots = true))
  }

  test("a dying hull yields its melee duel, a scratched one does not") {
    assert(retreat(totalHp = 60, attackingMelee = true))
    assert( ! retreat(totalHp = 61, attackingMelee = true))
    assert( ! retreat(totalHp = 60, attackingMelee = false))
  }

  test("a healthy hull without a reason stands") {
    assert( ! retreat())
    assert( ! retreat(totalHp = 200, attackingMelee = true))
  }

  test("blades need no formation, guns keep the dress-line gate") {
    assert( ! MeleeKiteAway.requiresFormation(melee = true))
    assert(MeleeKiteAway.requiresFormation(melee = false))
  }
}
