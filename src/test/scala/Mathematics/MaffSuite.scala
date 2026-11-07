package Mathematics

import org.scalatest.funsuite.AnyFunSuite

class MaffSuite extends AnyFunSuite {

  test("mode picks the clear majority") {
    assert(Maff.mode(Seq(true, true, false)) == true)
    assert(Maff.mode(Seq(false, false, true)) == false)
  }

  test("modeOpt is empty only for empty input") {
    assert(Maff.modeOpt(Seq.empty[Boolean]).isEmpty)
    assert(Maff.modeOpt(Seq(true)).contains(true))
  }

  test("a tied vote resolves deterministically (squad consensus depends on it)") {
    val first = Maff.mode(Seq(true, false))
    (1 to 100).foreach(_ => assert(Maff.mode(Seq(true, false)) == first))
  }

  test("minBy and maxBy on empty input return None instead of throwing") {
    assert(Maff.minBy(Seq.empty[Int])(identity).isEmpty)
    assert(Maff.maxBy(Seq.empty[Int])(identity).isEmpty)
  }

  test("minBy and maxBy pick the extremum through the macro path") {
    assert(Maff.minBy(Seq(3, 1, 2))(identity).contains(1))
    assert(Maff.maxBy(Seq(3, 1, 2))(identity).contains(3))
  }

  test("orElse takes the first non-empty input") {
    assert(Maff.orElse(Seq.empty[Int], Seq(1, 2)) == Seq(1, 2))
    assert(Maff.orElse(Seq(1), Seq(2)) == Seq(1))
  }

  test("clamp pins to the nearer bound regardless of bound order") {
    assert(Maff.clamp(5, 0, 10) == 5)
    assert(Maff.clamp(-5, 0, 10) == 0)
    assert(Maff.clamp(99, 0, 10) == 10)
    assert(Maff.clamp(99, 10, 0) == 10, "bounds may be swapped")
    assert(Maff.clamp01(-3) == 0)
    assert(Maff.clamp01(7) == 1)
  }
}
