package Debugging

import org.scalatest.funsuite.AnyFunSuite

/**
 * Hits-taken ledger (kite discipline, owner direction: a dueling Dragoon
 * is never hit more than a few times): frames where a warrior's total
 * hit points drop count as hits taken, one per frame no matter how many
 * guns landed. Regen never erases the count - a healed-over swing still
 * happened. Counted per unit, reported once - the owner reads it off
 * every UMS validation game next to the waste ledger.
 */
class HitsTakenSuite extends AnyFunSuite {

  test("clean ledger reports the all-clear") {
    HitsTaken.reset()
    assert(HitsTaken.summary().contains("none"))
    assert(HitsTaken.worstTotal == 0)
  }

  test("a fresh hull takes no hits") {
    HitsTaken.reset()
    HitsTaken.bump(7, "Dragoon", totalHp = 180)
    HitsTaken.bump(7, "Dragoon", totalHp = 180)
    assert(HitsTaken.worstTotal == 0)
  }

  test("every hp drop counts once, regen never erases") {
    HitsTaken.reset()
    HitsTaken.bump(7, "Dragoon", totalHp = 180)
    HitsTaken.bump(7, "Dragoon", totalHp = 164)
    assert(HitsTaken.worstTotal == 1)
    HitsTaken.bump(7, "Dragoon", totalHp = 165)
    HitsTaken.bump(7, "Dragoon", totalHp = 149)
    assert(HitsTaken.worstTotal == 2)
  }

  test("bumps accumulate per unit and name the worst") {
    HitsTaken.reset()
    HitsTaken.bump(7, "Dragoon", totalHp = 180)
    HitsTaken.bump(7, "Dragoon", totalHp = 172)
    HitsTaken.bump(7, "Dragoon", totalHp = 172)
    HitsTaken.bump(7, "Dragoon", totalHp = 160)
    HitsTaken.bump(9, "Zealot", totalHp = 160)
    HitsTaken.bump(9, "Zealot", totalHp = 140)
    val report = HitsTaken.summary()
    assert(report.contains("Dragoon #7"))
    assert(report.contains("2"))
    assert(HitsTaken.worstTotal == 2)
  }
}
