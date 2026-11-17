package Gameplans.Protoss.PvP

import org.scalatest.funsuite.AnyFunSuite

class MirrorGoonTechSuite extends AnyFunSuite {

  test("goon mirrors skip the reaver detour") {
    assert(MirrorGoonTech.skipReavers(fourGateGoon = true, threeGateGoon = false))
    assert(MirrorGoonTech.skipReavers(fourGateGoon = false, threeGateGoon = true))
    assert(MirrorGoonTech.skipReavers(fourGateGoon = true, threeGateGoon = true))
  }

  test("anything else keeps its tech reaction") {
    assert( ! MirrorGoonTech.skipReavers(fourGateGoon = false, threeGateGoon = false))
  }
}
