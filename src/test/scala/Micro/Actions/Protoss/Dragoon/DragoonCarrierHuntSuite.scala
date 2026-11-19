package Micro.Actions.Protoss.Dragoon

import org.scalatest.funsuite.AnyFunSuite

class DragoonCarrierHuntSuite extends AnyFunSuite {

  test("a shielded Dragoon always answers a Carrier in range") {
    assert(DragoonCarrierHunt.shouldShoot(isDragoon = true, shields = 20, carrierInRange = true))
    assert(DragoonCarrierHunt.shouldShoot(isDragoon = true, shields = 80, carrierInRange = true))
  }

  test("no shields, no duel, no range, no shot") {
    assert( ! DragoonCarrierHunt.shouldShoot(isDragoon = false, shields = 80, carrierInRange = true))
    assert( ! DragoonCarrierHunt.shouldShoot(isDragoon = true, shields = 19, carrierInRange = true))
    assert( ! DragoonCarrierHunt.shouldShoot(isDragoon = true, shields = 80, carrierInRange = false))
  }

  test("a shielded Dragoon closes a pathed Carrier inside eleven tiles") {
    assert(DragoonCarrierHunt.shouldClose(isDragoon = true, shields = 20, carrierTiles = 11.0, pathed = true))
    assert(DragoonCarrierHunt.shouldClose(isDragoon = true, shields = 80, carrierTiles = 3.0, pathed = true))
  }

  test("a dying hull keeps its retreat instead of trading") {
    assert(DragoonCarrierHunt.healthyEnough(totalHp = 71))
    assert( ! DragoonCarrierHunt.healthyEnough(totalHp = 70))
    assert( ! DragoonCarrierHunt.healthyEnough(totalHp = 0))
  }

  test("no path, no shields or no Dragoon means no chase") {
    assert( ! DragoonCarrierHunt.shouldClose(isDragoon = true, shields = 80, carrierTiles = 3.0, pathed = false))
    assert( ! DragoonCarrierHunt.shouldClose(isDragoon = true, shields = 80, carrierTiles = 11.1, pathed = true))
    assert( ! DragoonCarrierHunt.shouldClose(isDragoon = true, shields = 19, carrierTiles = 3.0, pathed = true))
    assert( ! DragoonCarrierHunt.shouldClose(isDragoon = false, shields = 80, carrierTiles = 3.0, pathed = true))
  }
}
