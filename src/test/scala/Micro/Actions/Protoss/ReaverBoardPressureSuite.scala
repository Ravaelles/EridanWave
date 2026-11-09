package Micro.Actions.Protoss

import org.scalatest.funsuite.AnyFunSuite

class ReaverBoardPressureSuite extends AnyFunSuite {

  private val idle = ReaverSituation()

  test("a loaded Reaver never boards, even doomed") {
    assert( ! ReaverBoardPressure.applies(idle.copy(loaded = true, doomed = true)))
  }

  test("a committed attack never boards, even doomed") {
    assert( ! ReaverBoardPressure.applies(idle.copy(committed = true, doomed = true)))
  }

  test("a doomed Reaver boards") {
    assert(ReaverBoardPressure.applies(idle.copy(doomed = true)))
  }

  test("a Scarab-targeted Reaver boards") {
    assert(ReaverBoardPressure.applies(idle.copy(targetedByScarab = true)))
  }

  test("cooling and outranged boards") {
    assert(ReaverBoardPressure.applies(idle.copy(cooling = true, outranged = true)))
  }

  test("a healthy Reaver with a shot to fire holds") {
    assert( ! ReaverBoardPressure.applies(idle.copy(scarabs = 5)))
  }
}
