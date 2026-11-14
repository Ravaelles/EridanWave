package Placement.Access

import Mathematics.Points.Tile

/**
 * Hug rule (no game access): inside an anchor rectangle's reach, a
 * foundation must touch it edge to edge; outside the reach the rule says
 * nothing. Written for the natural Pylon - a Supply Pylon anywhere within
 * ten tiles of the hall sits against the hall, so the wall's tiles and the
 * base's own footprint stay free - but it is generic rectangle geometry.
 *
 * Distance is the Chebyshev gap between the two footprints: 0 when they
 * touch or overlap, N when they are N tiles apart on the worse axis. The
 * same cheap metric as nearTile/farTiles, so the per-frame placement sweep
 * stays free of path queries.
 */
case class NearAdjacent(anchor: Tile, anchorWidth: Int, anchorHeight: Int, radiusTiles: Int) {

  def accepts(tile: Tile, width: Int, height: Int): Boolean = {
    val gap = gapTiles(tile, width, height)

    gap > radiusTiles || gap == 0
  }

  /** Chebyshev gap between this foundation's footprint and the anchor. */
  private def gapTiles(tile: Tile, width: Int, height: Int): Int = {
    val gapX = Math.max(anchor.x - (tile.x + width), tile.x - (anchor.x + anchorWidth))
    val gapY = Math.max(anchor.y - (tile.y + height), tile.y - (anchor.y + anchorHeight))

    Math.max(gapX, gapY)
  }
}
