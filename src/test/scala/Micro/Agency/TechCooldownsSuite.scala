package Micro.Agency

import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

class TechCooldownsSuite extends AnyFunSuite {

  test("melds pace the chain, mines wait out the plant") {
    assert(TechCooldowns.after(Protoss.ArchonMeld).contains(48))
    assert(TechCooldowns.after(Protoss.DarkArchonMeld).contains(48))
    assert(TechCooldowns.after(Terran.SpiderMinePlant).contains(12))
  }

  test("other techs keep the caller's pace and no recast guard") {
    assert(TechCooldowns.after(Protoss.PsionicStorm).isEmpty)
    assert(TechCooldowns.recastFrames(Protoss.PsionicStorm) == 0)
    assert(TechCooldowns.recastFrames(Terran.Stim) == 24)
  }
}
