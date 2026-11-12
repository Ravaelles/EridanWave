package Placement.Walls

import Mathematics.Points.Tile
import Placement.Walls.WallProblems.{IntersectsHall, IntersectsMining, IntersectsPrevious, UnbuildableGranular, UnbuildableTerrain}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import testkit.TestKit

class WallPlacementSuite extends AnyFunSuite with BeforeAndAfterAll {

  override def beforeAll(): Unit = TestKit.seedWorld(mapTiles = 256)

  private val at = WallPlacement.Rect(10, 10, 2, 2)

  private def terrain(buildable: Boolean = true, creep: Boolean = false): Seq[WallPlacement.Terrain] =
    at.tiles.map(t => WallPlacement.Terrain(t, buildable, creep))

  private def blocker(
    terrainBuildable: Boolean = true,
    buildings: Seq[WallPlacement.Rect] = Seq.empty,
    townHalls: Seq[WallPlacement.Rect] = Seq.empty,
    miningTiles: Set[Tile] = Set.empty,
    tiles: Seq[WallPlacement.Terrain] = terrain(),
    creepBlocks: Boolean = false
  ) = WallPlacement.blocker(at, terrainBuildable, buildings, townHalls, miningTiles, tiles, creepBlocks)

  test("a clear spot is legal") {
    assert(blocker() == None)
  }

  test("unbuildable terrain wins over every other rule") {
    assert(blocker(
      terrainBuildable = false,
      buildings = Seq(WallPlacement.Rect(10, 10, 2, 2)),
      tiles = terrain(buildable = false)) == Some(UnbuildableTerrain))
  }

  test("a building overlapping the spot is rejected") {
    assert(blocker(buildings = Seq(WallPlacement.Rect(11, 11, 3, 3))) == Some(IntersectsPrevious))
  }

  test("a building sharing only an edge is fine") {
    assert(blocker(buildings = Seq(WallPlacement.Rect(12, 10, 2, 2))) == None)
    assert(blocker(buildings = Seq(WallPlacement.Rect(8, 10, 2, 2))) == None)
  }

  test("the town hall is rejected even when no building overlaps") {
    assert(blocker(townHalls = Seq(WallPlacement.Rect(9, 9, 8, 8))) == Some(IntersectsHall))
  }

  test("a resource path tile under the footprint is rejected") {
    assert(blocker(miningTiles = at.tiles.toSet) == Some(IntersectsMining))
  }

  test("a footprint tile that is not buildable is rejected") {
    assert(blocker(tiles = terrain(buildable = false)) == Some(UnbuildableGranular))
  }

  test("creep only blocks non-Zerg builders") {
    assert(blocker(tiles = terrain(creep = true), creepBlocks = true) == Some(UnbuildableGranular))
    assert(blocker(tiles = terrain(creep = true), creepBlocks = false) == None)
  }

  test("the footprint covers width times height tiles") {
    assert(at.tiles.size == 4)
    assert(at.tiles.toSet == Set(Tile(10, 10), Tile(11, 10), Tile(10, 11), Tile(11, 11)))
  }
}
