package Lifecycle

import org.scalatest.funsuite.AnyFunSuite

class HopelessGameSuite extends AnyFunSuite {

  private val lost = (512 * 128, 2, 2, 24, 5.0)

  private def concede(frame: Int, workers: Int, ours: Int, enemy: Int, strength: Double): Boolean =
    HopelessGame.shouldConcede(frame, workers, ours, enemy, strength)

  test("a routed late game concedes") {
    assert(concede(lost._1, lost._2, lost._3, lost._4, lost._5))
  }

  test("a workerless game concedes early") {
    assert(concede(128, 0, 2, 24, 5.0))
  }

  test("a fresh game never concedes") {
    assert( ! concede(128, 10, 4, 0, 100.0))
  }

  test("off-throttle frames never concede") {
    assert( ! concede(lost._1 + 1, lost._2, lost._3, lost._4, lost._5))
  }

  test("a loss before the late mark holds on") {
    assert( ! concede(64 * 128, 12, 2, 24, 5.0))
  }

  test("a healthy relative score holds on") {
    assert( ! concede(lost._1, 12, 2, 24, 50.0))
  }

  test("a scout party does not end the game") {
    assert( ! concede(lost._1, lost._2, 1, 14, lost._5))
  }

  test("a winnable count holds") {
    assert( ! concede(lost._1, lost._2, 4, lost._4, lost._5))
  }

  test("a rebuildable remnant holds on") {
    assert( ! concede(lost._1, 29, 2, lost._4, lost._5))
  }
}
