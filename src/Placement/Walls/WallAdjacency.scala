package Placement.Walls

import Mathematics.Points.Point
import ProxyBwapi.UnitClasses.UnitClass

/** Geometry of two buildings sitting next to each other in a wall.
  *
  * A "margin" is the uncovered part of a building's tile box on one side. The
  * down and right margins are inclusive because unit dimensions are.
  */
object WallAdjacency {

  case class Margins(up: Int, downInclusive: Int, left: Int, rightInclusive: Int)

  case class Alleys(left: Int, right: Int, up: Int, down: Int) {
    /** True when the blocked unit fits through an alley, so this wall leaks. */
    def leaksTo(unitWidth: Int, unitHeight: Int): Boolean =
      left >= unitWidth || right >= unitWidth || up >= unitHeight || down >= unitHeight
  }

  val none: Alleys = Alleys(-1, -1, -1, -1)

  def marginsOf(unitClass: UnitClass): Margins =
    Margins(unitClass.marginUp, unitClass.marginDownInclusive, unitClass.marginLeft, unitClass.marginRightInclusive)

  /** Offset of the gap tile created when a building is brushed along one edge of
    * its tile box, pushed outward by the gaps spent on that side.
    */
  def gapOffset(relative: Point, width: Int, gapsUsed: Int): Point = {
    if      (relative.x == 0)           Point(- gapsUsed,   0)
    else if (relative.y == 0)           Point(  0,        - gapsUsed)
    else if (relative.x == width + 1)   Point(  gapsUsed,   0)
    else                                Point(  0,          gapsUsed)
  }

  /** Alleys between a building and the previously placed one. A side that is
    * not flush is a real gap, not an alley, and gets -1.
    */
  def alleys(last: WallPlacement.Rect, lastMargins: Margins, next: WallPlacement.Rect, nextMargins: Margins): Alleys = {
    val gapLeft  = next.x - last.x - last.width
    val gapRight = last.x - next.x - next.width
    val gapUp    = next.y - last.y - last.height
    val gapDown  = last.y - next.y - next.height
    Alleys(
      left  = if (gapLeft  == 0) lastMargins.rightInclusive + nextMargins.left            else -1,
      right = if (gapRight == 0) lastMargins.left           + nextMargins.rightInclusive  else -1,
      up    = if (gapUp    == 0) lastMargins.downInclusive  + nextMargins.up              else -1,
      down  = if (gapDown  == 0) lastMargins.up             + nextMargins.downInclusive   else -1)
  }
}
