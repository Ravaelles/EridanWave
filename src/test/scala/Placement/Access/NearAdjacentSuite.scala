package Placement.Access

import Mathematics.Points.Tile
import org.scalatest.funsuite.AnyFunSuite

class NearAdjacentSuite extends AnyFunSuite {

  // Anchor at (10,10), two wide and tall, reach five.
  private val rule = NearAdjacent(Tile(10, 10), anchorWidth = 2, anchorHeight = 2, radiusTiles = 5)

  test("a touching foundation is always welcome") {
    assert(rule.accepts(Tile(8, 10), width = 2, height = 2))
    assert(rule.accepts(Tile(12, 12), width = 2, height = 2))
    assert(rule.accepts(Tile(10, 12), width = 2, height = 2))
  }

  test("a loose foundation inside the reach is refused") {
    assert( ! rule.accepts(Tile(4, 10), width = 2, height = 2))
    assert( ! rule.accepts(Tile(10, 4), width = 1, height = 1))
  }

  test("past the reach the rule says nothing") {
    assert(rule.accepts(Tile(0, 0), width = 2, height = 2))
    assert(rule.accepts(Tile(20, 20), width = 2, height = 2))
  }
}
