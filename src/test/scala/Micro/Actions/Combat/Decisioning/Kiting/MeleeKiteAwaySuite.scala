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

  test("calm healthy-enough blades hold instead of kiting") {
    assert(MeleeKiteAway.holdsWhenCalm(framesSinceDamage = 41, totalHp = 42))
    assert(MeleeKiteAway.holdsWhenCalm(framesSinceDamage = 400, totalHp = 160))
    assert( ! MeleeKiteAway.holdsWhenCalm(framesSinceDamage = 40, totalHp = 160))
    assert( ! MeleeKiteAway.holdsWhenCalm(framesSinceDamage = 41, totalHp = 41))
    assert( ! MeleeKiteAway.holdsWhenCalm(framesSinceDamage = 0, totalHp = 160))
  }

  test("blades whose mates average less hold instead of kiting") {
    assert(MeleeKiteAway.standsWithWeakerPack(mateHpTotal = 60, mateCount = 2, myHp = 100))
    assert(MeleeKiteAway.standsWithWeakerPack(mateHpTotal = 30, mateCount = 1, myHp = 31))
    assert( ! MeleeKiteAway.standsWithWeakerPack(mateHpTotal = 0, mateCount = 0, myHp = 100))
    assert( ! MeleeKiteAway.standsWithWeakerPack(mateHpTotal = 100, mateCount = 2, myHp = 50))
    assert( ! MeleeKiteAway.standsWithWeakerPack(mateHpTotal = 200, mateCount = 2, myHp = 100))
  }

  test("a detached blade holds formation instead of dueling alone") {
    assert(MeleeKiteAway.holdsForPack(isMelee = true, hasPack = true, nearestMateTiles = 6.1, nearestFoeTiles = 7.0))
    assert(MeleeKiteAway.holdsForPack(isMelee = true, hasPack = true, nearestMateTiles = 60.0, nearestFoeTiles = 0.0))
    assert( ! MeleeKiteAway.holdsForPack(isMelee = false, hasPack = true, nearestMateTiles = 6.1, nearestFoeTiles = 7.0))
    assert( ! MeleeKiteAway.holdsForPack(isMelee = true, hasPack = false, nearestMateTiles = 99.0, nearestFoeTiles = 7.0))
    assert( ! MeleeKiteAway.holdsForPack(isMelee = true, hasPack = true, nearestMateTiles = 6.0, nearestFoeTiles = 7.0))
    assert( ! MeleeKiteAway.holdsForPack(isMelee = true, hasPack = true, nearestMateTiles = 60.0, nearestFoeTiles = 7.1))
  }
}
