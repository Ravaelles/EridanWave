package Micro.Actions.Protoss.DarkTemplar

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class DarkTemplarEvadeSuite extends AnyFunSuite {

  test("a Templar inside detection sight evades") {
    assert(DarkTemplarEvade.shouldEvade(0.0, 256.0))
    assert(DarkTemplarEvade.shouldEvade(255.0, 256.0))
  }

  test("the two-tile avoidance band evades before the sight touches") {
    assert(DarkTemplarEvade.shouldEvade(256.0 + 63.0, 256.0))
    assert( ! DarkTemplarEvade.shouldEvade(256.0 + 64.0, 256.0))
    assert( ! DarkTemplarEvade.shouldEvade(256.0 + 200.0, 256.0))
  }

  test("melee reach never holds the hide") {
    assert( ! DarkTemplarEvade.shouldHide(0.0, 32.0))
    assert( ! DarkTemplarEvade.shouldHide(0.0, 16.0))
  }

  test("a ranged threat inside reach plus two tiles holds the hide") {
    // Marine reach (4 tiles) + 2-tile band: 5 tiles hide, 7 tiles hunt.
    assert(DarkTemplarEvade.shouldHide(5.0 * 32.0, 4.0 * 32.0))
    assert( ! DarkTemplarEvade.shouldHide(7.0 * 32.0, 4.0 * 32.0))
  }

  test("a lone detector never moves a fresh hunt") {
    assert(DarkTemplarEvade.shouldHoldVsDetector(shieldsDown = 0, attackerNear = false))
    assert( ! DarkTemplarEvade.shouldHoldVsDetector(shieldsDown = 1, attackerNear = false))
    assert( ! DarkTemplarEvade.shouldHoldVsDetector(shieldsDown = 0, attackerNear = true))
  }

  test("guns outrank eyes inside thirteen tiles") {
    assert(DarkTemplarEvade.shouldFleeAttacker(distTiles = 0.0))
    assert(DarkTemplarEvade.shouldFleeAttacker(distTiles = 13.0))
    assert( ! DarkTemplarEvade.shouldFleeAttacker(distTiles = 13.01))
  }

  test("a revealed or shot Templar withdraws, a hidden one hunts on") {
    assert(DarkTemplarEvade.shouldWithdrawRevealed(enemyDetected = true, underAttack = false))
    assert(DarkTemplarEvade.shouldWithdrawRevealed(enemyDetected = false, underAttack = true))
    assert(DarkTemplarEvade.shouldWithdrawRevealed(enemyDetected = true, underAttack = true))
    assert( ! DarkTemplarEvade.shouldWithdrawRevealed(enemyDetected = false, underAttack = false))
  }

  test("the latch enters sharp and releases past the margin") {
    DarkTemplarEvade.reset()
    val sight = 256.0
    val enter = DarkTemplarEvade.evadeRadiusPixels(sight)
    val release = DarkTemplarEvade.evadeReleasePixels(sight)
    assert(release == enter + 2.0 * 32.0)
    assert(DarkTemplarEvade.evadeLatched(1, enter - 1.0, sight))
    assert(DarkTemplarEvade.evadeLatched(1, enter + 1.0, sight))
    assert( ! DarkTemplarEvade.evadeLatched(1, release + 1.0, sight))
    assert( ! DarkTemplarEvade.evadeLatched(1, enter + 1.0, sight))
  }

  test("a fresh unit only evades past the sharp edge") {
    DarkTemplarEvade.reset()
    val sight = 256.0
    val enter = DarkTemplarEvade.evadeRadiusPixels(sight)
    assert(DarkTemplarEvade.evadeLatched(7, enter - 1.0, sight))
    assert( ! DarkTemplarEvade.evadeLatched(8, enter + 1.0, sight))
    assert( ! DarkTemplarEvade.evadeLatched(8, enter + 1.0, 0.0))
  }

  test("covered targets are skipped, open ones hunt") {
    val detector = Pixel(0, 0)
    val sight = 256.0
    val covered = Pixel(100, 0)
    val open = Pixel(10000, 0)
    assert(DarkTemplarEvade.underDetection(covered, Seq((detector, sight))))
    assert( ! DarkTemplarEvade.underDetection(open, Seq((detector, sight))))
    assert( ! DarkTemplarEvade.underDetection(covered, Seq.empty))
  }

  test("an undetected Templar never yields the guns") {
    assert( ! DarkTemplarEvade.shouldYieldGuns(totalHp = 140, enemyDetected = false, hasVessel = true, attackedRecently = true))
    assert( ! DarkTemplarEvade.shouldYieldGuns(totalHp = 20, enemyDetected = false, hasVessel = true, attackedRecently = true))
  }

  test("a healthy Templar holds through detection") {
    assert(DarkTemplarEvade.yieldHp == 120)
    assert( ! DarkTemplarEvade.shouldYieldGuns(totalHp = 121, enemyDetected = true, hasVessel = true, attackedRecently = true))
    assert( ! DarkTemplarEvade.shouldYieldGuns(totalHp = 200, enemyDetected = true, hasVessel = false, attackedRecently = false))
  }

  test("a hurt Templar yields only to live detection with an answer") {
    assert(DarkTemplarEvade.shouldYieldGuns(totalHp = 120, enemyDetected = true, hasVessel = true, attackedRecently = false))
    assert(DarkTemplarEvade.shouldYieldGuns(totalHp = 50, enemyDetected = true, hasVessel = false, attackedRecently = true))
    assert( ! DarkTemplarEvade.shouldYieldGuns(totalHp = 50, enemyDetected = true, hasVessel = false, attackedRecently = false))
    assert( ! DarkTemplarEvade.shouldYieldGuns(totalHp = 50, enemyDetected = false, hasVessel = true, attackedRecently = false))
  }
}
