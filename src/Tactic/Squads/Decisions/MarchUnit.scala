package Tactic.Squads.Decisions

import Lifecycle.With
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
  verdictRetreat      : Boolean,
  verdictDuel         : Boolean,
  hasDuel             : Boolean,
  nearestWarriorTiles : Option[Double],
  distHomeTiles       : Option[Double],
  nearestArmedTiles   : Double,
  nearestStaticTiles  : Option[Double],
  nearestVisibleArmedTiles : Double)

object MarchUnit {

  def read(squad: Squad, unit: FriendlyUnitInfo, home: Option[Pixel], verdictRetreat: Boolean, verdictDuel: Boolean = false): MarchUnit =
    MarchUnit(
      unitId              = unit.id,
      isDarkTemplar       = Protoss.DarkTemplar(unit),
      isCorsair           = Protoss.Corsair(unit),
      isDragoon           = Protoss.Dragoon(unit),
      isWarrior           = unit.unitClass.isWarrior,
      isLoadedShuttle     = Protoss.Shuttle(unit) && unit.loadedUnitCount > 0,
      isSuicide           = SuicideMission.isActive(unit),
      shouldFight         = unit.agent.shouldFight,
      verdictRetreat      = verdictRetreat,
      verdictDuel         = verdictDuel,
      // The duel the legs would hold, read here so the gate and the legs
      // agree by construction: a hittable target even with detection on,
      // or incoming fire. Ghosts never qualify (recall horizon drops
      // them before they arm), unhittable watchers neither.
      hasDuel             = unit.agent.toAttack.exists(t =>
        Micro.Targeting.Target.holdsDuelTarget(unit.unitClass.isWarrior, unit.canAttack(t, assumeDetection = true))) ||
        unit.matchups.engagedUpon,
      nearestWarriorTiles = Maff.minBy(squad.units.view.filter(u => u != unit && u.unitClass.isWarrior))(_.pixelDistanceCenter(unit.pixel))
        .map(a => unit.pixelDistanceCenter(a.pixel) * Maff.inv32),
      distHomeTiles       = home.map(h => unit.pixel.groundPixels(h) * Maff.inv32),
      nearestArmedTiles   = Maff.minBy(With.units.enemy.view.filter(e =>
        e.alive && e.likelyStillThere && e.unitClass.canAttack(unit)))(_.pixelDistanceCenter(unit.pixel))
        .map(e => e.pixelDistanceCenter(unit.pixel) * Maff.inv32).getOrElse(Double.PositiveInfinity),
      nearestStaticTiles  = Maff.minBy(unit.matchups.enemies.view.filter(e =>
        e.likelyStillThere
        && e.unitClass.isStaticDefense
        && e.unitClass.canAttack(unit)))(unit.tileDistanceEdge).map(unit.tileDistanceEdge),
      // Visible guns that could hit this hull, in tiles, edge to edge
      // like the sibling gun scans: centers overstate clearance by up
      // to a tile against big hulls, and the cordon band is only two.
      // Reads only what the hull sees, so fogged memories never pin it
      // (ghosts own the recall horizon instead).
      nearestVisibleArmedTiles = Maff.minBy(With.units.enemy.view.filter(e =>
        e.alive && e.visible && e.unitClass.canAttack(unit.unitClass)))(_.pixelDistanceEdge(unit.pixel))
        .map(e => unit.pixelDistanceEdge(e.pixel) * Maff.inv32).getOrElse(Double.PositiveInfinity))
}
