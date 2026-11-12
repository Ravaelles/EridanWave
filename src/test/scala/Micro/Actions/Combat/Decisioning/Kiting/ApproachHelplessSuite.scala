package Micro.Actions.Combat.Decisioning.Kiting

import org.scalatest.funsuite.AnyFunSuite

class ApproachHelplessSuite extends AnyFunSuite {

  test("harmless air always closes inside the radius") {
    assert(ApproachHelpless.shouldClose(cantHitUs = true, flying = true, fullHealth = false, groundDistTiles = 11.9))
    assert(ApproachHelpless.shouldClose(cantHitUs = true, flying = true, fullHealth = true, groundDistTiles = 0.0))
    assert( ! ApproachHelpless.shouldClose(cantHitUs = true, flying = true, fullHealth = false, groundDistTiles = 12.01))
  }

  test("buildings close only at full health") {
    assert(ApproachHelpless.shouldClose(cantHitUs = true, flying = false, fullHealth = true, groundDistTiles = 5.0))
    assert( ! ApproachHelpless.shouldClose(cantHitUs = true, flying = false, fullHealth = false, groundDistTiles = 5.0))
    assert( ! ApproachHelpless.shouldClose(cantHitUs = true, flying = false, fullHealth = true, groundDistTiles = 12.01))
  }

  test("armed targets never count as helpless") {
    assert( ! ApproachHelpless.shouldClose(cantHitUs = false, flying = true, fullHealth = true, groundDistTiles = 1.0))
    assert( ! ApproachHelpless.shouldClose(cantHitUs = false, flying = false, fullHealth = true, groundDistTiles = 1.0))
  }
}
