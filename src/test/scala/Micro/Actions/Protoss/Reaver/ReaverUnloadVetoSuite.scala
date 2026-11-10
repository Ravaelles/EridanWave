package Micro.Actions.Protoss.Reaver

import org.scalatest.funsuite.AnyFunSuite

class ReaverUnloadVetoSuite extends AnyFunSuite {

  test("two ranged guns in margin still drop") {
    assert( ! ReaverUnloadVeto.veto(0))
    assert( ! ReaverUnloadVeto.veto(1))
    assert( ! ReaverUnloadVeto.veto(2))
  }

  test("three ranged guns veto the drop") {
    assert(ReaverUnloadVeto.veto(3))
    assert(ReaverUnloadVeto.veto(9))
  }
}
