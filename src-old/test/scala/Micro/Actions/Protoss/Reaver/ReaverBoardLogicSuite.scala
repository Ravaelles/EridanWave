package Micro.Actions.Protoss.Reaver

import org.scalatest.funsuite.AnyFunSuite

class ReaverBoardLogicSuite extends AnyFunSuite {

  test("cooling and outranged boards") {
    assert(ReaverBoardLogic.shouldBoardCooling(cooling = true, outranged = true, shotless = false, pressured = false))
  }

  test("cooling and shotless boards under pressure") {
    assert(ReaverBoardLogic.shouldBoardCooling(cooling = true, outranged = false, shotless = true, pressured = true))
  }

  test("cooling and shotless holds on an empty battlefield") {
    assert( ! ReaverBoardLogic.shouldBoardCooling(cooling = true, outranged = false, shotless = true, pressured = false))
  }

  test("cooling with a shot to fire holds") {
    assert( ! ReaverBoardLogic.shouldBoardCooling(cooling = true, outranged = false, shotless = false, pressured = true))
  }

  test("a ready Reaver never boards for cooldown") {
    assert( ! ReaverBoardLogic.shouldBoardCooling(cooling = false, outranged = true, shotless = true, pressured = true))
  }

  test("fighters nearby never stand") {
    assert( ! ReaverBoardLogic.shouldStandVsWorkers(onlyWorkersNear = false, shields = 80))
  }

  test("seventy shields still board against workers") {
    assert( ! ReaverBoardLogic.shouldStandVsWorkers(onlyWorkersNear = true, shields = 70))
  }

  test("above seventy shields the Reaver stands and shoots workers") {
    assert(ReaverBoardLogic.shouldStandVsWorkers(onlyWorkersNear = true, shields = 71))
    assert(ReaverBoardLogic.shouldStandVsWorkers(onlyWorkersNear = true, shields = 80))
  }
}
