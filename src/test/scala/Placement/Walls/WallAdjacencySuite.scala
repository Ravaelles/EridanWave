package Placement.Walls

import Mathematics.Points.Point
import org.scalatest.funsuite.AnyFunSuite

class WallAdjacencySuite extends AnyFunSuite {

  private val tight = WallAdjacency.Margins(up = 3, downInclusive = 3, left = 3, rightInclusive = 3)
  private val last  = WallPlacement.Rect(10, 10, 3, 3)

  private def alleysAt(x: Int, y: Int): WallAdjacency.Alleys =
    WallAdjacency.alleys(last, tight, WallPlacement.Rect(x, y, 3, 3), tight)

  test("gap offsets push the building outward by the gaps spent on that side") {
    // Relative points are the ring of a 3x3 tile box: edges at 0 and 4.
    assert(WallAdjacency.gapOffset(Point(0, 2), width = 3, gapsUsed = 0) == Point(0, 0))
    assert(WallAdjacency.gapOffset(Point(4, 2), width = 3, gapsUsed = 0) == Point(0, 0))
    assert(WallAdjacency.gapOffset(Point(1, 0), width = 3, gapsUsed = 0) == Point(0, 0))
    // Left and up edges push away from the horizon tile, right and down toward it.
    assert(WallAdjacency.gapOffset(Point(0, 2), width = 3, gapsUsed = 2) == Point(-2, 0))
    assert(WallAdjacency.gapOffset(Point(4, 2), width = 3, gapsUsed = 2) == Point(2, 0))
    assert(WallAdjacency.gapOffset(Point(1, 0), width = 3, gapsUsed = 2) == Point(0, -2))
    assert(WallAdjacency.gapOffset(Point(2, 4), width = 3, gapsUsed = 2) == Point(0, 2))
  }

  test("alley width is both buildings' margins, and only on flush sides") {
    assert(alleysAt(13, 10) == WallAdjacency.Alleys(left = 6, right = -1, up = -1, down = -1))
    assert(alleysAt(10, 13) == WallAdjacency.Alleys(left = -1, right = -1, up = 6, down = -1))
    assert(alleysAt(7, 10)  == WallAdjacency.Alleys(left = -1, right = 6, up = -1, down = -1))
    assert(alleysAt(10, 7)  == WallAdjacency.Alleys(left = -1, right = -1, up = -1, down = 6))
  }

  test("a one-tile gap between buildings is not an alley") {
    assert(alleysAt(14, 10) == WallAdjacency.none)
    assert(alleysAt(10, 14) == WallAdjacency.none)
  }

  test("a wall leaks only when the blocked unit fits through an alley") {
    val alley = WallAdjacency.Alleys(left = 23, right = -1, up = -1, down = -1)
    assert(alley.leaksTo(unitWidth = 23, unitHeight = 23))
    assert( ! alley.leaksTo(unitWidth = 24, unitHeight = 24))
    assert( ! WallAdjacency.none.leaksTo(unitWidth = 1, unitHeight = 1))
    // The unit's height decides for vertical alleys.
    assert(WallAdjacency.Alleys(-1, -1, up = 23, down = -1).leaksTo(unitWidth = 40, unitHeight = 23))
  }
}
