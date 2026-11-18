package Micro.Actions.Combat.Decisioning.Kiting

import org.scalatest.funsuite.AnyFunSuite

class MeleeKiteAwaySuite extends AnyFunSuite {

  private def retreat(
    totalHp: Int = 200,
    coolingDown: Boolean = true,
    hasLingNearby: Boolean = false,
    outnumberedByZealots: Boolean = false,
    attackingMelee: Boolean = false): Boolean =
    MeleeKiteAway.shouldRetreat(totalHp, coolingDown, hasLingNearby, outnumberedByZealots, attackingMelee)

  test("a ready gun never yields ground") {
    assert( ! retreat(coolingDown = false, hasLingNearby = true))
    assert( ! retreat(coolingDown = false, outnumberedByZealots = true))
    assert( ! retreat(totalHp = 10, coolingDown = false, attackingMelee = true))
  }

  test("lings nearby break the line at any health") {
    assert(retreat(totalHp = 200, hasLingNearby = true))
    assert(retreat(totalHp = 10, hasLingNearby = true))
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
