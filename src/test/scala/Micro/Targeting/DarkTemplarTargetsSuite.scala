package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class DarkTemplarTargetsSuite extends AnyFunSuite {

  private def mark(dist: Double, hp: Int, tank: Boolean = false, warrior: Boolean = true, worker: Boolean = false): DarkTemplarTargets.Mark =
    DarkTemplarTargets.Mark(dist, hp, tank, warrior, worker)

  test("weak targets go first even when farther") {
    assert(DarkTemplarTargets.pick(Seq(mark(5.0, 100), mark(9.0, 39))).contains(1))
    assert(DarkTemplarTargets.pick(Seq(mark(5.0, 100), mark(9.0, 40))).contains(1))
    assert(DarkTemplarTargets.pick(Seq(mark(5.0, 100), mark(9.0, 41))).contains(0))
  }

  test("tanks outrank warriors outrank workers outrank the rest") {
    assert(DarkTemplarTargets.pick(Seq(mark(1.0, 200, warrior = true), mark(9.0, 200, tank = true))).contains(1))
    assert(DarkTemplarTargets.pick(Seq(mark(9.0, 200, warrior = true), mark(1.0, 200, warrior = false, worker = true))).contains(0))
    assert(DarkTemplarTargets.pick(Seq(mark(1.0, 200, warrior = false), mark(9.0, 200, warrior = false, worker = true), mark(5.0, 200, warrior = true))).contains(2))
  }

  test("nearest inside the tier wins, nothing means nothing") {
    assert(DarkTemplarTargets.pick(Seq(mark(9.0, 35), mark(2.0, 35))).contains(1))
    assert(DarkTemplarTargets.pick(Seq.empty).isEmpty)
  }
}
