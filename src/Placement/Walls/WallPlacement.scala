package Placement.Walls

import Mathematics.Maff
import Mathematics.Points.Tile
import Mathematics.Shapes.Rectangle
import Placement.Walls.WallProblems.{IntersectsHall, IntersectsMining, IntersectsPrevious, UnbuildableGranular, UnbuildableTerrain, WallProblem}

/**
 * Clearance rules for dropping a building into a wall, with the game reads
 * lifted out: the caller passes terrain facts, so the order in which a
 * candidate is rejected can be argued with in a test instead of in a game.
 */
object WallPlacement {

  /** A building's footprint in tile coordinates. */
  case class Rect(x: Int, y: Int, width: Int, height: Int) {
    def endsX: Int = x + width
    def endsY: Int = y + height
    def tiles: Seq[Tile] = Rectangle(width, height).map(Tile(x, y).add)
    def intersects(other: Rect): Boolean =
      Maff.rectanglesIntersect(x, y, endsX, endsY, other.x, other.y, other.endsX, other.endsY)
  }

  /** What the map says about one tile of the footprint. */
  case class Terrain(tile: Tile, buildable: Boolean, creep: Boolean)

  /**
   * First rule that rejects this footprint, or `None` when it is legal.
   * Order is deliberate: terrain first (costly to test, cheapest to explain),
   * then overlaps, then the tile-level blockers.
   */
  def blocker(
    at: Rect,
    terrainBuildable: Boolean,
    buildings: Seq[Rect],
    townHalls: Seq[Rect],
    miningTiles: Set[Tile],
    terrain: Seq[Terrain],
    creepBlocks: Boolean
  ): Option[WallProblem] = {
    if ( ! terrainBuildable)                                    return Some(UnbuildableTerrain)
    if (buildings.exists(at.intersects))                        return Some(IntersectsPrevious)
    if (townHalls.exists(at.intersects))                        return Some(IntersectsHall)
    if (terrain.exists(t => miningTiles.contains(t.tile)))      return Some(IntersectsMining)
    if (terrain.exists(t => ! t.buildable || (creepBlocks && t.creep))) return Some(UnbuildableGranular)

    None
  }
}
