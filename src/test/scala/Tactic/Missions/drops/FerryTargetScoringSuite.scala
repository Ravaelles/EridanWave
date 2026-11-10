package Tactic.Missions.drops

import org.scalatest.funsuite.AnyFunSuite

class FerryTargetScoringSuite extends AnyFunSuite {

  test("sieged tanks win over farther ones, unsieged pay extra distance") {
    assert(FerryTargetScoring.score(100.0, sieged = true, 1.0, airThreat = false, 0.0).contains(100.0))
    assert(FerryTargetScoring.score(100.0, sieged = false, 1.0, airThreat = false, 0.0).contains(150.0))
    assert(FerryTargetScoring.score(200.0, sieged = true, 1.0, airThreat = false, 0.0).contains(200.0))
  }

  test("damaged tanks score better, floored near death") {
    assert(FerryTargetScoring.score(100.0, sieged = true, 0.5, airThreat = false, 0.0).contains(200.0))
    assert(FerryTargetScoring.score(100.0, sieged = true, 0.1, airThreat = false, 0.0).contains(400.0))
  }

  test("air threat vetoes distant drops, ground mass vetoes all") {
    assert(FerryTargetScoring.score(100.0, sieged = true, 1.0, airThreat = true, 0.0).isEmpty)
    assert(FerryTargetScoring.score(32.0, sieged = true, 1.0, airThreat = true, 0.0).isDefined)
    assert(FerryTargetScoring.score(100.0, sieged = true, 1.0, airThreat = false, 31.0).isEmpty)
    assert(FerryTargetScoring.score(100.0, sieged = true, 1.0, airThreat = false, 30.0).isDefined)
  }
}
