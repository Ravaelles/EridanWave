package Tactic.Missions.evaluation

import org.scalatest.funsuite.AnyFunSuite

class MissionVerdictSuite extends AnyFunSuite {

  test("profit over cost is success and repeats") {
    assert(MissionVerdict.evaluate(1, 0, wonGame = false) == MissionVerdict.Success)
    assert(MissionVerdict.evaluate(500, 100, wonGame = false) == MissionVerdict.Success)
  }

  test("a twenty-second harass succeeds by worker delay alone") {
    assert(MissionVerdict.evaluate(0, 480, wonGame = false) == MissionVerdict.Success)
    assert(MissionVerdict.evaluate(-200, 960, wonGame = false) == MissionVerdict.Success)
  }

  test("dead inside ten seconds is failure") {
    assert(MissionVerdict.evaluate(0, 240, wonGame = false) == MissionVerdict.Failure)
    assert(MissionVerdict.evaluate(-50, 0, wonGame = false) == MissionVerdict.Failure)
  }

  test("between ten and twenty seconds is unclear") {
    assert(MissionVerdict.evaluate(0, 300, wonGame = false) == MissionVerdict.Unclear)
  }

  test("a won game upgrades one level") {
    assert(MissionVerdict.evaluate(-50, 0, wonGame = true) == MissionVerdict.Unclear)
    assert(MissionVerdict.evaluate(0, 300, wonGame = true) == MissionVerdict.Success)
    assert(MissionVerdict.evaluate(1, 0, wonGame = true) == MissionVerdict.Success)
  }

  test("a minute denying mining succeeds with zero kills") {
    assert(MissionVerdict.evaluate(-50, 100, wonGame = false, workerHarassFrames = 1440) == MissionVerdict.Success)
  }

  test("partial pressure moves the needle linearly") {
    assert(MissionVerdict.evaluate(-50, 100, wonGame = false, workerHarassFrames = 720) == MissionVerdict.Unclear)
    assert(MissionVerdict.evaluate(0, 300, wonGame = false, workerHarassFrames = 1440) == MissionVerdict.Success)
    assert(MissionVerdict.evaluate(0, 300, wonGame = false, workerHarassFrames = 100) == MissionVerdict.Unclear)
    assert(MissionVerdict.evaluate(-50, 100, wonGame = false, workerHarassFrames = 0) == MissionVerdict.Failure)
  }
}
