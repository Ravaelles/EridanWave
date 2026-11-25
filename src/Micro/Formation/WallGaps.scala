package Micro.Formation

import Information.Geography.Types.Base
import Lifecycle.With
import Mathematics.Points.{Pixel, Tile, TileRectangle}
import Micro.Formation.WallGapGeometry.{Body, Box, Defender}
import Placement.Access.{Foundation, PlaceLabels}
import ProxyBwapi.Races.{Protoss, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import bwapi.UnitType

// Assigns melee defenders to the actual collision gaps in an occupied natural wall.
class WallGaps(base: Base) {
  private var previousBuildings : Vector[(Box, Boolean)] = Vector.empty
  private var previousDoor      : Option[Pixel]          = None
  private var layout            : Option[WallGapLayout]  = None

  def waypoint(unit: FriendlyUnitInfo, target: Pixel): Pixel = layout
    .map(_.waypoint(Defender(unit.id, Body.of(unit.unitClass), Protoss.Probe(unit), unit.pixel), target)).getOrElse(target)

  // Minimum Zealots the current placement plan needs to seal every detected entrance.
  // None means no active wall, or the planner cannot seal it.
  def minimumDefenders: Option[Int] = updateLayout().flatMap(_.minimumDefenders)

  def apply(units: Iterable[FriendlyUnitInfo]): Map[FriendlyUnitInfo, Pixel] = {
    val defenders = units.filter(u => u.alive && u.complete && ! u.loaded && u.isAny(Protoss.Zealot, Protoss.Probe)).toVector
    if (defenders.isEmpty) return Map.empty

    val currentLayout = updateLayout()
    if (currentLayout.isEmpty) return Map.empty

    val byId = defenders.map(u => u.id -> u).toMap
    currentLayout.get.assign(defenders.map(u => Defender(u.id, Body.of(u.unitClass), Protoss.Probe(u), u.pixel)))
      .map { case (id, pixel) => byId(id) -> pixel }
  }

  private def updateLayout(): Option[WallGapLayout] = {
    if ( ! base.isNatural || ! With.enemies.exists(_.isZerg)) {
      layout = None

      return None
    }
    // A wall may stand beyond the natural's zone; its hallway connects it to the base.
    val baseWall = With.placement.wall.filter(_.hallway.exists(_.zone == base.zone))
    val labeledFoundations: Seq[Foundation] = With.placement.get(label = PlaceLabels.Wall)
    val currentFoundations: Seq[Foundation] = labeledFoundations
      .flatMap((f: Foundation) => With.placement.get(tile = f.tile).toSeq)
    val foundations: Seq[Foundation] = currentFoundations.distinct.filter(f =>
      f.point.requirement.labels.contains(PlaceLabels.Wall) &&
        (baseWall.exists(_.buildings.exists(_._1 == f.tile)) ||
          new TileRectangle(f.tile, f.point.requirement.width, f.point.requirement.height).tiles.exists(_.zone == base.zone)))
    val wall = With.units.ours.filter(u => u.alive && ! u.flying && u.unitClass.isBuilding && foundations.exists(f =>
      f.tile == u.tileTopLeft && f.point.requirement.dimensions == ((u.unitClass.tileWidth, u.unitClass.tileHeight)))).toVector
    if ( ! wall.exists(u => u.unitClass.tileWidth == 4 && u.unitClass.tileHeight == 3)
      || ! wall.exists(u => u.unitClass.tileWidth == 3 && u.unitClass.tileHeight == 2)) {
      layout = None

      return None
    }

    val wallBoxes = wall.filterNot(_.unitClass.isStaticDefense).map(u => Body.of(u.unitClass).at(u.pixel))
    val vicinity = Box(wallBoxes.map(_.left).min, wallBoxes.map(_.top).min,
      wallBoxes.map(_.right).max, wallBoxes.map(_.bottom).max).expand(288)
    val buildings = With.units.all.filter(u => u.alive && ! u.flying && (u.unitClass.isBuilding || u.unitClass.isMinerals))
      .map(u => Body.of(u.unitClass).at(u.pixel)).filter(_.intersects(vicinity)).toVector.distinct
      .sortBy(b => (b.left, b.top, b.right, b.bottom)).map(b => (b, wallBoxes.contains(b)))
    val door = With.placement.wall.filter(w => w.buildings.exists(b => wall.exists(_.tileTopLeft == b._1))).flatMap(_.gap).map(_.center)
    if (layout.isEmpty || buildings != previousBuildings || door != previousDoor) {
      previousBuildings = buildings
      previousDoor = door
      layout = Some(new WallGapLayout(buildings, base.townHallArea.center, door,
        (x, y) => x >= 0 && y >= 0 && x < With.mapTileWidth * 4 && y < With.mapTileHeight * 4 && With.game.isWalkable(x, y),
        p => {
          val requirement = With.placement.at(p.tile).requirement
          requirement.walkableAfter && ! requirement.buildableAfter
        }))
    }
    layout
  }

  // One layout per foundation snapshot: the terrain, entrances, slot search
  // and sealing plan memoized exactly like the old Geometry, so repeat
  // frames cost lookups instead of searches.
  private class WallGapLayout(
    buildings: Vector[(Box, Boolean)],
    inside: Pixel,
    door: Option[Pixel],
    walkable: (Int, Int) => Boolean,
    hallway: Pixel => Boolean) {

    private val boxes: Vector[Box] = buildings.map(_._1)
    private val walls: Vector[Box] = buildings.filter(_._2).map(_._1)
    private val terrain = new WalkTerrain(
      boxes,
      Box(walls.map(_.left).min, walls.map(_.top).min, walls.map(_.right).max, walls.map(_.bottom).max).expand(96),
      walkable)
    private val ling = Body.of(Zerg.Zergling)
    private val zealotType = UnitType.Protoss_Zealot
    private val zealot = Body(zealotType.dimensionLeft, zealotType.dimensionUp, zealotType.dimensionRight + 1, zealotType.dimensionDown + 1)
    private def fits(body: Body, p: Pixel, clearance: Int): Boolean = terrain.fits(body, p, clearance)
    private def reachable(body: Body, p: Pixel): Boolean = terrain.navigationFor(body, inside).reachable(p)

    // Include pylons on the front line, but exclude aisles behind the large wall
    // buildings. Cannons remain collision obstacles, never entrance endpoints.
    private val majorWalls = walls.filter(b => b.right - b.left >= 64 || b.bottom - b.top >= 48) match {
      case large if large.nonEmpty => large
      case _ => walls
    }
    private val wallCenter = Pixel(majorWalls.map(_.center.x).sum / majorWalls.size, majorWalls.map(_.center.y).sum / majorWalls.size)
    private val inward = inside.subtract(wallCenter)
    private def wallDepth(p: Pixel): Double = (p.x - wallCenter.x).toDouble * inward.x + (p.y - wallCenter.y).toDouble * inward.y
    private val wallBack = majorWalls.flatMap(b => Vector(Pixel(b.left, b.top), Pixel(b.right, b.top), Pixel(b.left, b.bottom), Pixel(b.right, b.bottom))).map(wallDepth).max
    private val rawGaps = walls.flatMap(a => (walls ++ terrain.blockedBoxes).filterNot(_ == a)
      .flatMap(b => WallGapGeometry.between(a, b, inside, door, boxes, hallway, ling, fits)))
    private val gaps = WallGapGeometry.dedupGaps(rawGaps
      .filter(g => wallDepth(g.center) <= wallBack && (g.doorway || walls.contains(g.second))))

    private val slotSearch = new WallGapSlots(ling, fits, reachable)
    private val allocator = new WallGapAllocator(gaps, ling, zealot, slotSearch, reachable, fits)

    def waypoint(unit: Defender, target: Pixel): Pixel =
      terrain.navigationFor(unit.body, inside).waypoint(unit.id, unit.pixel, target)

    def minimumDefenders: Option[Int] = allocator.minimumDefenders

    def assign(units: Vector[Defender]): Map[Int, Pixel] = allocator.assign(units)
  }
}
