package Micro.Actions.Combat.Decisioning.Kiting

import org.scalatest.funsuite.AnyFunSuite

class ApproachHelplessSuite extends AnyFunSuite {

  test("harmless air always closes inside the radius") {
    assert(ApproachHelpless.shouldClose(cantHitUs = true, flying = true, shieldWound = 80, groundDistTiles = 11.9))
    assert(ApproachHelpless.shouldClose(cantHitUs = true, flying = true, shieldWound = 0, groundDistTiles = 0.0))
    assert( ! ApproachHelpless.shouldClose(cantHitUs = true, flying = true, shieldWound = 80, groundDistTiles = 12.01))
  }

  test("grounded helpless close on scratched shields, not holed ones") {
    assert(ApproachHelpless.shouldClose(cantHitUs = true, flying = false, shieldWound = 0, groundDistTiles = 5.0))
    assert(ApproachHelpless.shouldClose(cantHitUs = true, flying = false, shieldWound = 15, groundDistTiles = 5.0))
    assert( ! ApproachHelpless.shouldClose(cantHitUs = true, flying = false, shieldWound = 16, groundDistTiles = 5.0))
    assert( ! ApproachHelpless.shouldClose(cantHitUs = true, flying = false, shieldWound = 0, groundDistTiles = 12.01))
  }

  test("armed targets never count as helpless") {
    assert( ! ApproachHelpless.shouldClose(cantHitUs = false, flying = true, shieldWound = 0, groundDistTiles = 1.0))
    assert( ! ApproachHelpless.shouldClose(cantHitUs = false, flying = false, shieldWound = 0, groundDistTiles = 1.0))
  }
}
