package Micro.Actions.Protoss.Reaver

import org.scalatest.funsuite.AnyFunSuite

class ReaverBraveStandSuite extends AnyFunSuite {

  private val idle = ReaverSituation()

  test("bravery needs shields and a stale shot") {
    assert( ! ReaverBraveStand.brave(shields = 49, framesSinceFired = 999999))
    assert( ! ReaverBraveStand.brave(shields = 80, framesSinceFired = 95))
    assert(ReaverBraveStand.brave(shields = 50, framesSinceFired = 96))
    assert(ReaverBraveStand.brave(shields = 80, framesSinceFired = 999999))
  }

  test("sieged-tank range plus margin is never entered") {
    assert(ReaverBraveStand.tankCovered(tankRangeTiles = 12.0, distTiles = 0.0))
    assert(ReaverBraveStand.tankCovered(tankRangeTiles = 12.0, distTiles = 13.0))
    assert( ! ReaverBraveStand.tankCovered(tankRangeTiles = 12.0, distTiles = 13.01))
  }

  test("a stale shielded Reaver with a foe in reach stands") {
    assert(ReaverBraveStand.applies(idle.copy(shields = 50, framesSinceFired = 96, scarabs = 1, foeInReach = true)))
    assert(ReaverBraveStand.applies(idle.copy(hasActiveScarab = true, foeInReach = true)))
  }

  test("a loaded or committed Reaver never stands") {
    assert( ! ReaverBraveStand.applies(idle.copy(loaded = true, shields = 80, framesSinceFired = 96, scarabs = 1, foeInReach = true)))
    assert( ! ReaverBraveStand.applies(idle.copy(committed = true, shields = 80, framesSinceFired = 96, scarabs = 1, foeInReach = true)))
  }

  test("doom, Scarabs, statics and tanks veto the stand") {
    assert( ! ReaverBraveStand.applies(idle.copy(doomed = true, shields = 80, framesSinceFired = 96, scarabs = 1, foeInReach = true)))
    assert( ! ReaverBraveStand.applies(idle.copy(targetedByScarab = true, shields = 80, framesSinceFired = 96, scarabs = 1, foeInReach = true)))
    assert( ! ReaverBraveStand.applies(idle.copy(staticInFace = true, shields = 80, framesSinceFired = 96, scarabs = 1, foeInReach = true)))
    assert( ! ReaverBraveStand.applies(idle.copy(tankCovered = true, shields = 80, framesSinceFired = 96, scarabs = 1, foeInReach = true)))
  }

  test("a fresh or shotless Reaver never stands") {
    assert( ! ReaverBraveStand.applies(idle.copy(shields = 49, framesSinceFired = 96, scarabs = 1, foeInReach = true)))
    assert( ! ReaverBraveStand.applies(idle.copy(shields = 80, framesSinceFired = 95, scarabs = 1, foeInReach = true)))
    assert( ! ReaverBraveStand.applies(idle.copy(shields = 80, framesSinceFired = 96, scarabs = 0, foeInReach = true)))
    assert( ! ReaverBraveStand.applies(idle.copy(shields = 80, framesSinceFired = 96, scarabs = 1, foeInReach = false)))
  }
}
