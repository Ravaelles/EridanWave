package Micro.Actions.Combat.Decisioning.Kiting

import org.scalatest.funsuite.AnyFunSuite

class KiteToSuite extends AnyFunSuite {

  test("harmless air always closes inside the radius") {
    assert(KiteTo.shouldClose(cantHitUs = true, flying = true, shieldWound = 80, groundDistTiles = 11.9))
    assert(KiteTo.shouldClose(cantHitUs = true, flying = true, shieldWound = 0, groundDistTiles = 0.0))
    assert( ! KiteTo.shouldClose(cantHitUs = true, flying = true, shieldWound = 80, groundDistTiles = 12.01))
  }

  test("grounded helpless close on scratched shields, not holed ones") {
    assert(KiteTo.shouldClose(cantHitUs = true, flying = false, shieldWound = 0, groundDistTiles = 5.0))
    assert(KiteTo.shouldClose(cantHitUs = true, flying = false, shieldWound = 15, groundDistTiles = 5.0))
    assert( ! KiteTo.shouldClose(cantHitUs = true, flying = false, shieldWound = 16, groundDistTiles = 5.0))
    assert( ! KiteTo.shouldClose(cantHitUs = true, flying = false, shieldWound = 0, groundDistTiles = 12.01))
  }

  test("armed targets never count as helpless") {
    assert( ! KiteTo.shouldClose(cantHitUs = false, flying = true, shieldWound = 0, groundDistTiles = 1.0))
    assert( ! KiteTo.shouldClose(cantHitUs = false, flying = false, shieldWound = 0, groundDistTiles = 1.0))
  }
}
