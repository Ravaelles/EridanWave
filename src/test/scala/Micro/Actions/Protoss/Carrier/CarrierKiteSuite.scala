package Micro.Actions.Protoss.Carrier

import org.scalatest.funsuite.AnyFunSuite

class CarrierKiteSuite extends AnyFunSuite {

  test("unscratched shields never kite") {
    assert( ! CarrierKite.shouldKite(shieldWounded = false, distTiles = 0.0, marginTiles = 15.0))
  }

  test("a fresh scratch holds a tight berth") {
    // Margin ~0 at zero wounds: anything outside it holds orders.
    assert( ! CarrierKite.shouldKite(shieldWounded = true, distTiles = 0.5, marginTiles = 0.5))
    assert(CarrierKite.shouldKite(shieldWounded = true, distTiles = 0.4, marginTiles = 0.5))
  }

  test("a bleeding hull keeps fifteen tiles") {
    assert(CarrierKite.shouldKite(shieldWounded = true, distTiles = 14.99, marginTiles = 15.0))
    assert( ! CarrierKite.shouldKite(shieldWounded = true, distTiles = 15.0, marginTiles = 15.0))
  }

  test("only true reach counts as ranged") {
    assert( ! CarrierKite.isRanged(32.0))
    assert(CarrierKite.isRanged(33.0))
  }
}
