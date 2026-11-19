package Micro.Actions.Combat.Maneuvering

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.Pixel
import Micro.Agency.Commander
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Dangerous-unit avoid, last in the avoid order: Effects (storms, mines)
 * steer first, deadly unseen killers (cloaked DT, burrowed Lurker) turn
 * the march next, and only then this tier answers. Its first rule is
 * Ghost Lockdown: a charged Ghost freezes any mechanical hull it
 * touches, so Carriers, Arbiters and Shuttles never stand inside
 * lockdown reach of one - a hull that cannot move is already dead, and
 * no trade is worth donating the hull for. Distances in tiles, energy
 * in game points; numbers pure for the suite, the Ghost scan is the
 * seam.
 */
object AvoidDangerousUnits {

  /** Lockdown reach of a Ghost, in tiles: never stand inside it. */
  val lockdownRadiusTiles: Double = 11.1

  /**
   * Lockdown-ready Ghost energy: the spell costs 100, and 98 covers the
   * regen during the approach - a hull waiting for the round hundred
   * eats the cast it was counting down.
   */
  val lockdownReadyEnergy: Int = 98

  /** Step past the line, in tiles: the fallback lands outside, not on it. */
  val pastLineTiles: Double = 1.0

  /** Hulls this tier answers for: the mechanical air the Ghost freezes. */
  def isLockdownPrey(unit: FriendlyUnitInfo): Boolean =
    unit.isAny(Protoss.Carrier, Protoss.Arbiter, Protoss.Shuttle)

  /** A live remembered Ghost charged enough to cast on approach. */
  def isLockdownThreat(ghostEnergy: Int, distTiles: Double): Boolean =
    ghostEnergy >= lockdownReadyEnergy && distTiles < lockdownRadiusTiles

  /**
   * Targeted check (pure ids for the suite): a Ghost with our hull on
   * its order target is already casting or about to - leave regardless
   * of range or battery. A set target with our id means the cast is
   * inbound; waiting for the energy readout trades the hull for a
   * number.
   */
  def isTargetedBy(ghostTargetId: Option[Int], victimId: Int): Boolean =
    ghostTargetId.contains(victimId)

  /** Raw fallback point, straight away from the Ghost past the line. */
  def fallback(own: Pixel, threat: Pixel): Pixel =
    own.project(threat, -(lockdownRadiusTiles + pastLineTiles) * 32.0)

  def act(unit: FriendlyUnitInfo): Boolean = {
    if ( ! isLockdownPrey(unit)) return false
    if ( ! unit.canMove) return false

    Maff.minBy(With.units.enemy.view.filter(e =>
      e.alive && e.likelyStillThere && Terran.Ghost(e) && (
        e.energy >= lockdownReadyEnergy || isTargetedBy(e.orderTarget.map(_.id), unit.id))))(
      unit.pixelDistanceEdge(_)).exists(ghost => {
      if (isTargetedBy(ghost.orderTarget.map(_.id), unit.id) ||
        isLockdownThreat(ghost.energy, unit.pixelDistanceEdge(ghost) * Maff.inv32)) {
        unit.cherryVisUnitLog("AvoidLockdown")
        unit.agent.forceTo(CornerAntiStuckGuard.guard(
          fallback(unit.pixel, ghost.pixel),
          With.mapPixelWidth,
          With.mapPixelHeight))
        Commander.move(unit)
        true
      } else false
    })
  }
}
