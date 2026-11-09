package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class TargetScoringSuite extends AnyFunSuite {

  test("air sizes route to air damages") {
    assert(TargetScoring.damageForSize(true, 2, 10, 20, 30, 40, 50, 60) == 10)
    assert(TargetScoring.damageForSize(true, 1, 10, 20, 30, 40, 50, 60) == 20)
    assert(TargetScoring.damageForSize(true, 0, 10, 20, 30, 40, 50, 60) == 30)
  }

  test("ground sizes route to ground damages") {
    assert(TargetScoring.damageForSize(false, 2, 10, 20, 30, 40, 50, 60) == 40)
    assert(TargetScoring.damageForSize(false, 1, 10, 20, 30, 40, 50, 60) == 50)
    assert(TargetScoring.damageForSize(false, 0, 10, 20, 30, 40, 50, 60) == 60)
  }
}
