package Micro.Actions.Protoss.DarkTemplar

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.Pixel
import Micro.Actions.Combat.Decisioning.NearestShot
import Micro.Actions.{Action, UnitControl}
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.Latch
import Utilities.?

/**
 * Dark Templar raid (surrounding preemptor, BeDarkTemplar untouched):
 * the brain walks remembered guns down and holds empty ground, so the
 * raid owns the two frames the brain gets wrong. Guns first: a live
 * cannon inside its close band is walked around, never into - unless
 * the shared duel math calls it clearly positive, in which case the
 * brain's chew stands. Workers second: an enemy worker inside a tile
 * and a twentieth always dies first, better than the base and better
 * than holding. One owner per frame: either leg claiming makes the
 * unit unready, so the brain never re-votes the same frame.
 */
object DarkTemplarRaid extends Action {

  /** Workers this close always die first, in tiles: better than the base. */
  val workerDefaultTiles: Double = 1.05

  def shouldTakeWorker(distTiles: Double): Boolean =
    distTiles <= workerDefaultTiles

  /** Guns are avoided inside their close band, never dived into. */
  def shouldAvoidCannon(distTiles: Double, dangerTiles: Double): Boolean =
    distTiles <= dangerTiles + DarkTemplarStaticStandoff.closeMarginTiles

  /** Release past the close band plus the exit margin: the escape commits. */
  def releaseDistTiles(dangerTiles: Double): Double =
    dangerTiles + DarkTemplarStaticStandoff.closeMarginTiles + DarkTemplarStaticStandoff.exitMarginTiles

  /**
   * Escape past the release band on the away ray: walkable snapping can
   * shave a tile, so the leg lands one past the band instead of on it -
   * a settled escape projects onto itself instead of oscillating.
   */
  def escapeFrom(own: Pixel, gun: Pixel, dangerTiles: Double): Pixel =
    own.project(gun, -(dangerTiles + DarkTemplarStaticStandoff.closeMarginTiles + DarkTemplarStaticStandoff.exitMarginTiles + 1.0) * 32.0)

  override def appliesToUnit(unit: FriendlyUnitInfo): Boolean = Protoss.DarkTemplar(unit)

  override def allowed(unit: FriendlyUnitInfo): Boolean =
    Protoss.DarkTemplar(unit) && unit.canMove && ! unit.loaded

  override protected def perform(unit: FriendlyUnitInfo): UnitControl = {
    if (avoidGuns(unit)) return UnitControl.Claimed
    if (takeWorkers(unit)) return UnitControl.Claimed

    UnitControl.Declined
  }

  private val avoid = new Latch

  private def avoidGuns(unit: FriendlyUnitInfo): Boolean = {
    val gun = Maff.minBy(With.units.enemy.view.filter(e =>
      e.alive && e.likelyStillThere && e.complete && e.unitClass.isStaticDefense && e.canAttack(unit)))(
      unit.pixelDistanceEdge(_))
    if (gun.isEmpty) { avoid(unit.id, fires = false, releases = true); return false }

    val g = gun.get
    val distTiles = unit.pixelDistanceEdge(g) * Maff.inv32
    val dangerTiles = DarkTemplarStaticStandoff.dangerTiles(g.pixelRangeAgainst(unit) * Maff.inv32, g.sightPixels * Maff.inv32)
    // Clearly positive evaluation fights the gun instead: the shared
    // duel math, same inputs as the hunt, so the raid never disagrees
    // with the brain about a free cannon. Latched like the standoff:
    // entry is sharp, release only past the band, so the push cannot
    // re-grab the hull every other frame on the boundary.
    if (duelWorthIt(unit, g)) { avoid(unit.id, fires = false, releases = true); return false }

    if ( ! avoid(unit.id,
      fires = shouldAvoidCannon(distTiles, dangerTiles),
      releases = distTiles > releaseDistTiles(dangerTiles))) return false

    unit.agent.act("DarkTemplarStaticAvoid")
    val dest = escapeFrom(unit.pixel, g.pixel, dangerTiles)
    unit.agent.forceTo(?(dest.walkable, dest, dest.walkablePixel))
    Commander.move(unit)

    true
  }

  private def duelWorthIt(unit: FriendlyUnitInfo, gun: UnitInfo): Boolean = {
    val pack = With.units.enemy.view.filter(c =>
      c.alive && gun.pixelDistanceEdge(c) * Maff.inv32 <= DarkTemplarBravery.packTiles).toSeq
    val ourDps = unit.unitClass.effectiveGroundDamage.toDouble / unit.unitClass.groundDamageCooldown.max(1) +
      unit.squad.toSeq.flatMap(_.units)
        .filter(u => u != unit && u.alive && u.unitClass.isWarrior && gun.pixelDistanceEdge(u) * Maff.inv32 <= DarkTemplarBravery.packTiles)
        .map(u => u.unitClass.effectiveGroundDamage.toDouble / u.unitClass.groundDamageCooldown.max(1)).sum
    val foeDps = ?(unit.effectivelyCloaked, 0.0, pack.map(f =>
      ?(f.unitClass.isStaticDefense && ! f.complete, 0.0, f.unitClass.effectiveGroundDamage.toDouble / f.unitClass.groundDamageCooldown.max(1))).sum)
    DarkTemplarBravery.shouldDuel(unit.totalHealth, ourDps, pack.map(_.totalHealth).sum, foeDps)
  }

  private def takeWorkers(unit: FriendlyUnitInfo): Boolean = {
    val workers = unit.matchups.targets.view.filter(t =>
      t.alive && t.visible && unit.canAttack(t) && t.unitClass.isWorker &&
        shouldTakeWorker(unit.pixelDistanceEdge(t) * Maff.inv32)).toSeq
    if (workers.isEmpty) return false

    NearestShot.take(unit, "HuntEnemyWorkers", workers)
  }
}
