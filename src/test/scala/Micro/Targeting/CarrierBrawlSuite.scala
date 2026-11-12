package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class CarrierBrawlSuite extends AnyFunSuite {

  test("fifty interceptors always brawl") {
    assert(CarrierBrawl.shouldBrawl(50))
    assert(CarrierBrawl.shouldBrawl(96))
    assert( ! CarrierBrawl.shouldBrawl(49))
    assert( ! CarrierBrawl.shouldBrawl(0))
  }

  test("the beat is nine tiles") {
    assert(CarrierBrawl.inBrawl(9.0))
    assert( ! CarrierBrawl.inBrawl(9.01))
  }
}
