package Micro.Actions.Protoss.Reaver

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

  test("a ready shot with an enemy nearby holds even when doomed") {
    assert( ! ReaverBoardPressure.applies(idle.copy(doomed = true, scarabs = 5, threatened = true)))
    assert( ! ReaverBoardPressure.applies(idle.copy(doomed = true, scarabs = 5, holdTarget = true)))
  }

  test("a doomed Reaver with a shot but no enemy still boards") {
    assert(ReaverBoardPressure.applies(idle.copy(doomed = true, scarabs = 5)))
  }

  test("in contain an unpressured Reaver holds even when doomed") {
    assert( ! ReaverBoardPressure.applies(idle.copy(doomed = true, scarabs = 0, cooling = true, containing = true)))
  }

  test("in contain an attacked Reaver still boards") {
    assert(ReaverBoardPressure.applies(idle.copy(doomed = true, cooling = true, containing = true, threatened = true)))
  }

  test("a spent Reaver still boards under pressure") {
    assert(ReaverBoardPressure.applies(idle.copy(doomed = true, cooling = true, threatened = true)))
  }

  test("only workers nearby with strong shields holds even doomed") {
    assert( ! ReaverBoardPressure.applies(idle.copy(doomed = true, onlyWorkersNear = true, shields = 80)))
    assert( ! ReaverBoardPressure.applies(idle.copy(threatened = true, pressuredClose = false, onlyWorkersNear = true, shields = 75)))
  }

  test("only workers nearby with weak shields still boards") {
    assert(ReaverBoardPressure.applies(idle.copy(doomed = true, onlyWorkersNear = true, shields = 70)))
    assert(ReaverBoardPressure.applies(idle.copy(doomed = true, onlyWorkersNear = true, shields = 0)))
  }
}
