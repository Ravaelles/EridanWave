package Debugging

import org.scalatest.funsuite.AnyFunSuite

class SlowAloneSuite extends AnyFunSuite {

  test("a cruising unit is not slow, even alone") {
    assert( ! SlowAlone.shouldNote(moving = true, speedNow = 100.0, topSpeed = 100.0, distTiles = 30.0, nearestAllyTiles = 99.0, nearestEnemyTiles = 99.0))
  }

  test("a stopped unit is not crawling without an order") {
    assert( ! SlowAlone.shouldNote(moving = false, speedNow = 0.0, topSpeed = 100.0, distTiles = 30.0, nearestAllyTiles = 99.0, nearestEnemyTiles = 99.0))
  }

  test("a crawling unit far from its goal with nobody around gets noted") {
    assert(SlowAlone.shouldNote(moving = true, speedNow = 20.0, topSpeed = 100.0, distTiles = 30.0, nearestAllyTiles = 99.0, nearestEnemyTiles = 99.0))
  }

  test("company nearby vetoes the note") {
    assert( ! SlowAlone.shouldNote(moving = true, speedNow = 20.0, topSpeed = 100.0, distTiles = 30.0, nearestAllyTiles = 9.9, nearestEnemyTiles = 99.0))
    assert( ! SlowAlone.shouldNote(moving = true, speedNow = 20.0, topSpeed = 100.0, distTiles = 30.0, nearestAllyTiles = 99.0, nearestEnemyTiles = 9.9))
  }

  test("an arrival is not a crawl") {
    assert( ! SlowAlone.shouldNote(moving = true, speedNow = 20.0, topSpeed = 100.0, distTiles = 4.0, nearestAllyTiles = 99.0, nearestEnemyTiles = 99.0))
  }
}
