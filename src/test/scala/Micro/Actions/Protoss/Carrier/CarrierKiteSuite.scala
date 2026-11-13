package Micro.Actions.Protoss.Carrier

import org.scalatest.funsuite.AnyFunSuite

class CarrierKiteSuite extends AnyFunSuite {

  test("a healthy Carrier never kites") {
    assert( ! CarrierKite.shouldKite(100, 0.0))
    assert( ! CarrierKite.shouldKite(400, 0.0))
  }

  test("a distant attacker never kites") {
    assert( ! CarrierKite.shouldKite(99, 14.0))
    assert( ! CarrierKite.shouldKite(1, 200.0))
  }

  test("a dying Carrier kites a close ranged attacker") {
    assert(CarrierKite.shouldKite(99, 13.99))
    assert(CarrierKite.shouldKite(0, 0.0))
  }

  test("only true reach counts as ranged") {
    assert( ! CarrierKite.isRanged(32.0))
    assert(CarrierKite.isRanged(33.0))
  }
}
