package Micro.Actions.Protoss.Dragoon

import org.scalatest.funsuite.AnyFunSuite

class DragoonPvpRetreatSuite extends AnyFunSuite {

  private def retreat(
    isDragoon: Boolean = true,
    enemyIsProtoss: Boolean = true,
    oursDragoons: Int = 3,
    enemyHasDragoons: Boolean = true,
    usUnits: Double = 10.0,
    foeUnits: Double = 10.0): Boolean =
    DragoonPvpRetreat.shouldRetreat(isDragoon, enemyIsProtoss, oursDragoons, enemyHasDragoons, usUnits, foeUnits)

  test("an outnumbered handful breaks off to the middle choke") {
    assert(retreat(usUnits = 10.0, foeUnits = 10.0))
    assert(retreat(usUnits = 13.0, foeUnits = 10.0))
    assert( ! retreat(usUnits = 14.0, foeUnits = 10.0))
    assert( ! retreat(usUnits = 20.0, foeUnits = 10.0))
  }

  test("only early Protoss mirrors with enemy Dragoons retreat") {
    assert( ! retreat(isDragoon = false))
    assert( ! retreat(enemyIsProtoss = false))
    assert( ! retreat(oursDragoons = 5))
    assert(retreat(oursDragoons = 4))
    assert( ! retreat(enemyHasDragoons = false))
    assert( ! retreat(foeUnits = 0.0))
  }
}
