package Tactic.Squads

import RegroupLatch.{Engaged, Regrouping}
import org.scalatest.funsuite.AnyFunSuite

class RegroupLatchSuite extends AnyFunSuite {

  private val fullCoverage = 1.0

  test("engaged squad regroups once clearly losing the sim") {
    assert(RegroupLatch.nextStance(Engaged, fightingFraction = 0.2, coverageFraction = fullCoverage) == Regrouping)
  }

  test("engaged squad holds through the deadband") {
    assert(RegroupLatch.nextStance(Engaged, fightingFraction = 0.5, coverageFraction = fullCoverage) == Engaged)
  }

  test("regrouping squad holds through the deadband") {
    assert(RegroupLatch.nextStance(Regrouping, fightingFraction = 0.5, coverageFraction = fullCoverage) == Regrouping)
  }

  test("regrouping squad re-engages only once clearly winning the sim") {
    assert(RegroupLatch.nextStance(Regrouping, fightingFraction = 0.7, coverageFraction = fullCoverage) == Engaged)
  }

  test("thresholds are strict: boundary values hold the stance") {
    assert(RegroupLatch.nextStance(Engaged, fightingFraction = RegroupLatch.startRegroupBelow, coverageFraction = fullCoverage) == Engaged)
    assert(RegroupLatch.nextStance(Regrouping, fightingFraction = RegroupLatch.stopRegroupAbove, coverageFraction = fullCoverage) == Regrouping)
  }

  test("thin sim coverage never flips either stance") {
    assert(RegroupLatch.nextStance(Engaged, fightingFraction = 0.0, coverageFraction = 0.2) == Engaged)
    assert(RegroupLatch.nextStance(Regrouping, fightingFraction = 1.0, coverageFraction = 0.2) == Regrouping)
  }

  test("no sim data at all keeps the stance") {
    assert(RegroupLatch.nextStance(Engaged, fightingFraction = 0.0, coverageFraction = 0.0) == Engaged)
    assert(RegroupLatch.nextStance(Regrouping, fightingFraction = 0.0, coverageFraction = 0.0) == Regrouping)
  }
}
