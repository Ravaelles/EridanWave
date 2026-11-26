package Micro.Actions.Protoss.Dragoon

import org.scalatest.funsuite.AnyFunSuite

class DragoonPvpRetreatSuite extends AnyFunSuite {

  private def retreat(
    isDragoon: Boolean = true,
    enemyIsProtoss: Boolean = true,
    oursDragoons: Int = 3,
    enemyHasDragoons: Boolean = true,
    usUnits: Double = 10.0,
    foeUnits: Double = 10.0,
    umsMode: Boolean = false): Boolean =
    DragoonPvpRetreat.shouldRetreat(isDragoon, enemyIsProtoss, oursDragoons, enemyHasDragoons, usUnits, foeUnits, umsMode)

  test("an outnumbered handful breaks off to the middle choke") {
    assert(retreat(usUnits = 10.0, foeUnits = 10.0))
    assert(retreat(usUnits = 13.0, foeUnits = 10.0))
    assert( ! retreat(usUnits = 14.0, foeUnits = 10.0))
    assert( ! retreat(usUnits = 20.0, foeUnits = 10.0))
  }

  test("a small line under Dragoon pressure reads twenty percent weaker") {
    assert(retreat(usUnits = 6.0, foeUnits = 4.0))
    assert( ! retreat(usUnits = 6.0, foeUnits = 3.0))
    assert( ! retreat(usUnits = 7.0, foeUnits = 5.0))
  }

  test("only early Protoss mirrors with enemy Dragoons retreat") {
    assert( ! retreat(isDragoon = false))
    assert( ! retreat(enemyIsProtoss = false))
    assert( ! retreat(oursDragoons = 5))
    assert(retreat(oursDragoons = 4))
    assert( ! retreat(enemyHasDragoons = false))
    assert( ! retreat(foeUnits = 0.0))
  }

  test("a lone dragoon walks home to the line (macro; UMS duels gate this at the caller)") {
    assert(retreat(oursDragoons = 1, usUnits = 1.0, foeUnits = 1.0))
  }

  test("UMS doubles the fight margin: the floor halves, parity-plus duels hold") {
    assert(retreat(usUnits = 10.0, foeUnits = 10.0, umsMode = false))
    assert( ! retreat(usUnits = 10.0, foeUnits = 10.0, umsMode = true))
    assert( ! retreat(usUnits = 7.0, foeUnits = 10.0, umsMode = true))
    assert(retreat(usUnits = 6.9, foeUnits = 10.0, umsMode = true))
    assert( ! retreat(isDragoon = false, umsMode = true))
  }
}
