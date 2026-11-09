package Micro.Actions.Protoss

import org.scalatest.funsuite.AnyFunSuite

class ReaverUnloadSafetySuite extends AnyFunSuite {

  test("a healthy Reaver needs two tiles") {
    assert(ReaverUnloadSafety.radiusTiles(0.0) == 2.0)
  }

  test("the light-wound boundary still needs two tiles") {
    assert(ReaverUnloadSafety.radiusTiles(0.33) == 2.0)
  }

  test("a chipped Reaver needs room") {
    assert(ReaverUnloadSafety.radiusTiles(0.5) == 3.9)
  }

  test("the heavy-wound boundary still holds narrower room") {
    assert(ReaverUnloadSafety.radiusTiles(0.6) == 3.9)
  }

  test("a mauled Reaver needs wide room") {
    assert(ReaverUnloadSafety.radiusTiles(0.9) == 5.7)
  }
}
