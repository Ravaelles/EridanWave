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
}
