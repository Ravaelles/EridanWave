package Micro.Targeting

import Micro.Targeting.ReaverTargets.Mark
import org.scalatest.funsuite.AnyFunSuite

class ReaverTargetsSuite extends AnyFunSuite {

  test("nearest detected killer wins, the unseen never do") {
    assert(ReaverTargets.pickNearestDetected(Seq(
      Mark(9.0, detected = true),
      Mark(3.0, detected = false),
      Mark(5.0, detected = true))) == Some(2))
    assert(ReaverTargets.pickNearestDetected(Seq(
      Mark(3.0, detected = false),
      Mark(9.0, detected = false))) == None)
    assert(ReaverTargets.pickNearestDetected(Seq.empty) == None)
  }
}
