package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class ReaverLeashSuite extends AnyFunSuite {

  test("a foot hull past three tiles rejoins the ball") {
    assert(ReaverLeash.shouldRejoin(distTiles = 3.01))
    assert(ReaverLeash.shouldRejoin(distTiles = 14.0))
  }

  test("a hull inside the ball holds its orders") {
    assert( ! ReaverLeash.shouldRejoin(distTiles = 3.0))
    assert( ! ReaverLeash.shouldRejoin(distTiles = 0.0))
  }
}
