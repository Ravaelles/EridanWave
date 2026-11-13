package Micro.Agency

import org.scalatest.funsuite.AnyFunSuite

class OrderContinueSuite extends AnyFunSuite {

  test("a far goal on the current heading holds its course") {
    assert(OrderContinue.holdsCourse(distTiles = 50.0, headingAligned = true))
    assert(OrderContinue.holdsCourse(distTiles = 4.01, headingAligned = true))
    assert( ! OrderContinue.holdsCourse(distTiles = 50.0, headingAligned = false))
  }

  test("a near goal never cruises: arrival owns it") {
    assert( ! OrderContinue.holdsCourse(distTiles = 4.0, headingAligned = true))
    assert( ! OrderContinue.holdsCourse(distTiles = 0.5, headingAligned = true))
  }
}
