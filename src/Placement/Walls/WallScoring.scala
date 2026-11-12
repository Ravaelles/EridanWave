package Placement.Walls

import Mathematics.Points.Tile

/**
 * How good a wall is: tight, aligned, close to the ramp, with the gap at an
 * end rather than on the far side. The weights live here; WallFinder only
 * measures the geometry.
 */
object WallScoring {

  /** Each tile of exposed perimeter costs this much. */
  val perimeterWeight: Double = 10.0

  /** Each tile of misalignment between buildings costs this much. */
  val alignmentWeight: Double = 12.0

  /** Misaligned inner edges matter twice as much: that is the open side. */
  val insideEdgeWeight: Double = 24.0

  /** Each tile of hall margin we are missing costs this much. */
  val hallMarginWeight: Double = 60.0

  /** Tiles of hall margin we get for free. */
  val comfortableHallMargin: Int = 6

  /** Each tile further from the ramp costs this much. */
  val distanceWeight: Double = 6.0

  def score(perimeter: Int, alignment: Double, insideEdgeSpread: Int, hallMargin: Int, gapPenalty: Int, rampDistance: Int): Double =
    perimeterWeight   * perimeter +
    alignmentWeight   * alignment +
    insideEdgeWeight  * insideEdgeSpread +
    hallMarginWeight  * Math.max(0, comfortableHallMargin - hallMargin) +
    gapPenalty -
    distanceWeight    * rampDistance

  def alignment(centersX2: Seq[Int], centersY2: Seq[Int]): Double =
    Math.min(centersX2.max - centersX2.min, centersY2.max - centersY2.min) / 2.0

  def isHorizontal(centersX2: Seq[Int], centersY2: Seq[Int]): Boolean =
    centersX2.max - centersX2.min >= centersY2.max - centersY2.min

  /**
   * Spread between the buildings' inner edges: the side the enemy walks in
   * from is the one facing away from our origin.
   */
  def insideEdgeSpread(rects: Seq[WallPlacement.Rect], horizontal: Boolean, origin: Tile): Int = {
    val centerX2 = rects.map(r => 2 * r.x + r.width).sum / rects.length
    val centerY2 = rects.map(r => 2 * r.y + r.height).sum / rects.length
    val edges = rects.map(r =>
      if (horizontal) { if (2 * origin.y < centerY2) r.y else r.y + r.height }
      else            { if (2 * origin.x < centerX2) r.x else r.x + r.width })
    edges.max - edges.min
  }

  /**
   * A gap flanked by two buildings is the point of the wall. A gap at the
   * near end still gets walked through; a gap on the far side of the wall
   * means the wall does not protect anything.
   */
  def gapPenalty(adjacentBuildings: Int, gapFartherThanWall: Boolean): Int =
    if (adjacentBuildings >= 2) 0
    else if (gapFartherThanWall) 400
    else 1200
}
