package Tactic.Squads.Decisions

import Mathematics.Maff
import Mathematics.Points.Pixel
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.Squad

/**
 * Per-unit facts the march gates read, measured once per unit instead of
 * once per gate: the same nearest-warrior scan, home distance and static
 * scan used to run inside every gate that wanted them. Reads relocate
 * here verbatim - distances keep their original metric (centers for the
 * pack, edges for the guns) - so the gates decide on numbers, never on
 * the game.
 */
final case class MarchUnit(
  unitId              : Int,
  isDarkTemplar       : Boolean,
  isCorsair           : Boolean,
  isDragoon           : Boolean,
  isWarrior           : Boolean,
  isLoadedShuttle     : Boolean,
  isSuicide           : Boolean,
  shouldFight         : Boolean,
  nearestWarriorTiles : Option[Double],
  distHomeTiles       : Option[Double],
  nearestStaticTiles  : Option[Double])

object MarchUnit {

  def read(squad: Squad, unit: FriendlyUnitInfo, home: Option[Pixel]): MarchUnit =
    MarchUnit(
      unitId              = unit.id,
      isDarkTemplar       = Protoss.DarkTemplar(unit),
      isCorsair           = Protoss.Corsair(unit),
      isDragoon           = Protoss.Dragoon(unit),
      isWarrior           = unit.unitClass.isWarrior,
      isLoadedShuttle     = Protoss.Shuttle(unit) && unit.loadedUnitCount > 0,
      isSuicide           = SuicideMission.isActive(unit),
      shouldFight         = unit.agent.shouldFight,
      nearestWarriorTiles = Maff.minBy(squad.units.view.filter(u => u != unit && u.unitClass.isWarrior))(_.pixelDistanceCenter(unit.pixel))
        .map(a => unit.pixelDistanceCenter(a.pixel) * Maff.inv32),
      distHomeTiles       = home.map(h => unit.pixel.groundPixels(h) * Maff.inv32),
      nearestStaticTiles  = Maff.minBy(unit.matchups.enemies.view.filter(e =>
        e.likelyStillThere
        && e.unitClass.isStaticDefense
        && e.unitClass.canAttack(unit)))(unit.tileDistanceEdge).map(unit.tileDistanceEdge))
}
