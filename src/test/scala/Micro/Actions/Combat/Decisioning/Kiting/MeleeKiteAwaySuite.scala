package Micro.Actions.Combat.Decisioning.Kiting

import org.scalatest.funsuite.AnyFunSuite

class MeleeKiteAwaySuite extends AnyFunSuite {

  private def retreat(
    totalHp: Int = 200,
    coolingDown: Boolean = true,
    hasSlowLingNearby: Boolean = false,
    hasSpeedlingNearby: Boolean = false,
    packIntact: Boolean = false,
    outnumberedByZealots: Boolean = false,
    attackingMelee: Boolean = false): Boolean =
    MeleeKiteAway.shouldRetreat(totalHp, coolingDown, hasSlowLingNearby, hasSpeedlingNearby, packIntact, outnumberedByZealots, attackingMelee)

  test("a ready gun never yields ground") {    assert( ! retreat(coolingDown = false, hasSlowLingNearby = true))
    assert( ! retreat(coolingDown = false, outnumberedByZealots = true))
    assert( ! retreat(totalHp = 10, coolingDown = false, attackingMelee = true))
  }

  test("slow lings nearby break the line at any health") {
    assert(retreat(totalHp = 200, hasSlowLingNearby = true))
    assert(retreat(totalHp = 10, hasSlowLingNearby = true))
  }

  test("speedlings break only a balled line, loners stand and swing") {
    assert(retreat(totalHp = 200, hasSpeedlingNearby = true, packIntact = true))
    assert( ! retreat(totalHp = 200, hasSpeedlingNearby = true, packIntact = false))
    assert( ! retreat(totalHp = 200, hasSpeedlingNearby = true))
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
