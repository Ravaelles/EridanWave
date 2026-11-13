package Micro.Actions.Protoss.Reaver

import org.scalatest.funsuite.AnyFunSuite

class ReaverBraveStandSuite extends AnyFunSuite {

  private val idle = ReaverSituation()

  private def standing(
    shields         : Int = 80,
    framesSinceFired: Int = 96,
    scarabs         : Int = 1,
    foeInReach      : Boolean = true): ReaverSituation =
    idle.copy(shields = shields, framesSinceFired = framesSinceFired, scarabs = scarabs, foeInReach = foeInReach)

  test("bravery needs shields and a stale shot") {
    assert( ! ReaverBraveStand.brave(shields = 49, framesSinceFired = 999999))
    assert( ! ReaverBraveStand.brave(shields = 80, framesSinceFired = 95))
    assert(ReaverBraveStand.brave(shields = 50, framesSinceFired = 96))
    assert(ReaverBraveStand.brave(shields = 80, framesSinceFired = 999999))
  }

  test("a stale shielded Reaver with a loaded Scarab and a foe in reach stands") {
    assert(ReaverBraveStand.applies(standing()))
    assert(ReaverBraveStand.applies(standing(shields = 50)))
  }

  test("a Scarab in flight is not a loaded shot") {
    assert( ! ReaverBraveStand.applies(standing().copy(hasActiveScarab = true)))
  }

  test("a cooling Reaver has nothing to stand for") {
    assert( ! ReaverBraveStand.applies(standing().copy(cooling = true)))
  }

  test("a loaded or committed Reaver never stands") {
    assert( ! ReaverBraveStand.applies(standing().copy(loaded = true)))
    assert( ! ReaverBraveStand.applies(standing().copy(committed = true)))
  }

  test("a foe close enough to chew it vetoes the stand") {
    assert( ! ReaverBraveStand.applies(standing().copy(pressuredClose = true)))
  }

  test("doom, Scarabs, statics and tanks veto the stand") {
    assert( ! ReaverBraveStand.applies(standing().copy(doomed = true)))
    assert( ! ReaverBraveStand.applies(standing().copy(targetedByScarab = true)))
    assert( ! ReaverBraveStand.applies(standing().copy(staticInFace = true)))
    assert( ! ReaverBraveStand.applies(standing().copy(tankCovered = true)))
  }

  test("a fresh, shotless or unreached Reaver never stands") {
    assert( ! ReaverBraveStand.applies(standing(shields = 49)))
    assert( ! ReaverBraveStand.applies(standing(framesSinceFired = 95)))
    assert( ! ReaverBraveStand.applies(standing(scarabs = 0)))
    assert( ! ReaverBraveStand.applies(standing(foeInReach = false)))
  }
}
