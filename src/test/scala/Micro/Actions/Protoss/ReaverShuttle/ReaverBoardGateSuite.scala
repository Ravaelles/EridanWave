package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class ReaverBoardGateSuite extends AnyFunSuite {

  private def locked(
    framesSinceShot: Int = ReaverBoardGate.quietFrames + 1,
    nearestEnemyTiles: Double = ReaverBoardGate.crowdRadiusTiles - 1): Boolean =
    ReaverBoardGate.locked(framesSinceShot, nearestEnemyTiles)

  test("a quiet crowded hull stays on the ground") {
    assert(locked())
  }

  test("a hull that shot recently still boards") {
    assert( ! locked(framesSinceShot = ReaverBoardGate.quietFrames))
    assert( ! locked(framesSinceShot = 0))
  }

  test("a quiet hull with room still boards") {
    assert( ! locked(nearestEnemyTiles = ReaverBoardGate.crowdRadiusTiles))
    assert(locked(nearestEnemyTiles = ReaverBoardGate.crowdRadiusTiles - 0.1))
  }
}
