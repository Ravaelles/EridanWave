package Micro.Actions.Protoss

import org.scalatest.funsuite.AnyFunSuite

class ReaverBoardLogicSuite extends AnyFunSuite {

  test("cooling and outranged boards") {
    assert(ReaverBoardLogic.shouldBoardCooling(cooling = true, outranged = true, shotless = false))
  }

  test("cooling and shotless boards") {
    assert(ReaverBoardLogic.shouldBoardCooling(cooling = true, outranged = false, shotless = true))
  }

  test("cooling with a shot to fire holds") {
    assert( ! ReaverBoardLogic.shouldBoardCooling(cooling = true, outranged = false, shotless = false))
  }

  test("a ready Reaver never boards for cooldown") {
    assert( ! ReaverBoardLogic.shouldBoardCooling(cooling = false, outranged = true, shotless = true))
  }
}
