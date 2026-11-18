package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class DemiseGateSuite extends AnyFunSuite {

  test("statics always qualify, dying or not") {
    assert(DemiseGate.qualifies(isStaticDefense = true, doomFar = true))
    assert(DemiseGate.qualifies(isStaticDefense = true, doomFar = false))
  }

  test("units follow the doom distance") {
    assert(DemiseGate.qualifies(isStaticDefense = false, doomFar = true))
    assert( ! DemiseGate.qualifies(isStaticDefense = false, doomFar = false))
  }
}
