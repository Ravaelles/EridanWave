package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class ReaverCoolWalkSuite extends AnyFunSuite {

  test("a cooling hull walks off a closing threat") {
    assert(ReaverCoolWalk.shouldWalk(cooldownLeft = 60, threatTiles = 0.0))
    assert(ReaverCoolWalk.shouldWalk(cooldownLeft = 9, threatTiles = 7.69))
    assert( ! ReaverCoolWalk.shouldWalk(cooldownLeft = 8, threatTiles = 0.0))
    assert( ! ReaverCoolWalk.shouldWalk(cooldownLeft = 60, threatTiles = 7.7))
    assert( ! ReaverCoolWalk.shouldWalk(cooldownLeft = 60, threatTiles = 40.0))
  }
}
