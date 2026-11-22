package Debugging

import org.scalatest.funsuite.AnyFunSuite

/**
 * Motion-mismatch ledger (L1): sustained frames where the technique duels
 * while the march holds Withdraw, or the march retreats while the duel is
 * held. Single frames are release noise (the bus absorbs three); twelve
 * consecutive frames mean the phases genuinely fight over the unit.
 * Counted, not probed - the Motion audit row and the Act chain bracket
 * the frames, the summary names the units.
 */
class MotionMismatchSuite extends AnyFunSuite {

  test("clean ledger reports the all-clear") {
    MotionMismatch.reset()
    assert(MotionMismatch.summary().contains("none"))
    assert(MotionMismatch.worstDuel == 0)
    assert(MotionMismatch.worstMarch == 0)
  }

  test("a release-window blip never reports") {
    MotionMismatch.reset()
    (0 until MotionMismatch.sustainedFrames - 1).foreach(f => MotionMismatch.bumpDuel(1, "Dragoon", f))
    assert(MotionMismatch.summary().contains("none"))
  }

  test("twelve consecutive duel-under-withdraw frames report once") {
    MotionMismatch.reset()
    (0 until MotionMismatch.sustainedFrames).foreach(f => MotionMismatch.bumpDuel(7, "Dragoon", f))
    val report = MotionMismatch.summary()
    assert(report.contains("Dragoon #7"))
    assert(report.contains("12f"))
    assert(MotionMismatch.worstDuel == MotionMismatch.sustainedFrames)
  }

  test("a broken streak restarts the count") {
    MotionMismatch.reset()
    (0 until MotionMismatch.sustainedFrames - 1).foreach(f => MotionMismatch.bumpDuel(3, "Zealot", f))
    MotionMismatch.bumpDuel(3, "Zealot", 100)
    MotionMismatch.bumpDuel(3, "Zealot", 101)
    assert(MotionMismatch.summary().contains("none"))
  }

  test("march-under-duel tracks its own streak") {
    MotionMismatch.reset()
    (0 until MotionMismatch.sustainedFrames).foreach(f => MotionMismatch.bumpMarch(9, "Dragoon", f))
    val report = MotionMismatch.summary()
    assert(report.contains("Dragoon #9"))
    assert(MotionMismatch.worstMarch == MotionMismatch.sustainedFrames)
    assert(MotionMismatch.worstDuel == 0)
  }
}
