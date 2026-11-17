package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class TargetRecallSuite extends AnyFunSuite {

  test("a remembered fix inside the horizon is recalled") {
    assert(TargetRecall.shouldRecall(0.0))
    assert(TargetRecall.shouldRecall(12.0))
    assert( ! TargetRecall.shouldRecall(12.01))
    assert( ! TargetRecall.shouldRecall(40.0))
  }
}
