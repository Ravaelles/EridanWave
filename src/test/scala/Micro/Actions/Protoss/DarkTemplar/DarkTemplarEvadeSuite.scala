package Micro.Actions.Protoss.DarkTemplar

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
}
