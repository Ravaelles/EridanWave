package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class FocusFireSuite extends AnyFunSuite {

  test("shots to kill rounds up and never returns zero") {
    assert(FocusFire.shotsToKill(100.0, 20.0) == 5)
    assert(FocusFire.shotsToKill(101.0, 20.0) == 6)
    assert(FocusFire.shotsToKill(5.0, 20.0) == 1)
  }

  test("no damage per shot cannot kill anything") {
    assert(FocusFire.shotsToKill(100.0, 0.0) == Int.MaxValue)
  }

  test("volleys are priced for the squad, not the shooter") {
    // Same total damage from different hulls: the same number of volleys.
    assert(FocusFire.volleysToKill(120.0, Seq(20, 20)) == 3)
    assert(FocusFire.volleysToKill(120.0, Seq(40)) == 3)
    assert(FocusFire.volleysToKill(120.0, Seq(5, 5, 5, 5)) == 6)
    // A stronger line needs fewer volleys for the same target.
    assert(FocusFire.volleysToKill(120.0, Seq(60)) == 2)
    assert(FocusFire.volleysToKill(120.0, Seq(20)) == 6)
    // No damage still means no kill.
    assert(FocusFire.volleysToKill(120.0, Seq(0)) == Int.MaxValue)
  }

  test("kill-ease counts the guns already on the target, plus one average volley") {
    // Nearly-dead with one gun on it dies sooner than fresh with two.
    assert(FocusFire.effectiveVolleys(healthWithShields = 40.0, committedDamage = 20.0, avgDamage = 20.0) == 1)
    assert(FocusFire.effectiveVolleys(healthWithShields = 160.0, committedDamage = 40.0, avgDamage = 20.0) == 3)
    // An untouched fresh hull waits behind a joined-up kill.
    assert(FocusFire.effectiveVolleys(healthWithShields = 140.0, committedDamage = 0.0, avgDamage = 20.0) == 7)
    assert(FocusFire.effectiveVolleys(healthWithShields = 150.0, committedDamage = 60.0, avgDamage = 20.0) == 2)
    // No guns, no average: no kill.
    assert(FocusFire.effectiveVolleys(healthWithShields = 100.0, committedDamage = 0.0, avgDamage = 0.0) == Int.MaxValue)
  }

  test("two guns already share a target, one mills alone") {
    assert( ! FocusFire.enoughShooters(1))
    assert(FocusFire.enoughShooters(2))
    assert(FocusFire.enoughShooters(14))
  }

  test("two targets left still share one pick, one is trivially shared") {
    assert( ! FocusFire.enoughTargets(1))
    assert(FocusFire.enoughTargets(2))
    assert(FocusFire.enoughTargets(4))
  }
}
