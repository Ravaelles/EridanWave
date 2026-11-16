package Gameplans.Eridan.PvZ

import org.scalatest.funsuite.AnyFunSuite

class ZealotIntoGoonSuite extends AnyFunSuite {

  test("gas and the Core go down once the line stands") {
    assert(ZealotIntoGoon.shouldTechUp(zealots = 6, target = 6, hasCore = false))
    assert(ZealotIntoGoon.shouldTechUp(zealots = 12, target = 6, hasCore = false))
  }

  test("no duplicate tech orders and no tech before the line") {
    assert( ! ZealotIntoGoon.shouldTechUp(zealots = 6, target = 6, hasCore = true))
    assert( ! ZealotIntoGoon.shouldTechUp(zealots = 5, target = 6, hasCore = false))
    assert( ! ZealotIntoGoon.shouldTechUp(zealots = 0, target = 2, hasCore = false))
  }
}
