package Micro.Actions.Protoss.ReaverShuttle

import Mathematics.{Maff, WoundMargin}
import Mathematics.Points.Pixel
import Utilities.Latch
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Pair flight geometry (no game access): a loaded Shuttle runs from
 * Scourge to the rear of its cluster, and turns around early when a
 * ranged threat's wound-scaled reach touches it. One wound curve prices
 * both the unsafe gate and the preemptive band, so a single knob retunes
 * them. Mirrors the MatchupAnalysis safety branches (64 skating, 160
 * standing).
 */
object ReaverShuttleFlight {

  /** Scourge this close, in tiles, send a loaded Shuttle running. */
  val scourgeDistTiles: Double = 12.0

  /** Preemptive band beyond a ranged threat's own reach, in tiles. */
  val baseFleeMarginTiles: Double = 4.0

  /** Extra preemptive band at full wounds, in tiles (woundBonus(0, 9)). */
  val woundFleeBonusTiles: Double = 9.0

  def woundBonusTiles(woundPercent: Double): Double =
    WoundMargin(0.0, woundFleeBonusTiles).tiles(woundPercent)

  /** Loaded Shuttle margin over the threat, in pixels: skate fresh, run hurt. */
  def safetyMarginPixels(woundPercent: Double, fasterFlyer: Boolean): Double =
    (if (fasterFlyer) 64 else 160) + woundBonusTiles(woundPercent) * 32.0

  /**
   * How far inside its trigger a threat sits, in pixels: positive means the
   * flee fires. A ranged threat's trigger is its reach plus the wound-scaled
   * band, so a Hydralisk walking up to a Shuttle in the enemy main turns it
   * around before the pack closes; an incoming Scourge gets its own radius.
   */
  def penetrationPixels(distPixels: Double, triggerPixels: Double): Double =
    triggerPixels - distPixels

  /**
   * Flee exit margin, in tiles: a fleeing hull keeps running until every
   * threat is this far past its trigger. The ferry destination (at the
   * enemy) directly opposes the flee destination (away), so a sharp edge
   * flips every time a pack breathes across it - a UMS duel shows
   * AirFlee/DuelFerry alternating ~100 times each, and the ride never
   * delivers in the losing run. Latched LoneMarch shape, keyed on the
   * worst penetration so a second threat cannot overwrite the hold.
   */
  val fleeExitMarginTiles: Double = 2.0

  def fleeExitMarginPixels: Double = fleeExitMarginTiles * 32.0

  /** How far inside its trigger the worst threat sits, in pixels. */
  def worstPenetration(penetrations: Seq[Double]): Double =
    penetrations.reduceOption(_ max _).getOrElse(Double.NegativeInfinity)

  private val fleeing = new Latch

  def reset(): Unit = { fleeing.reset() }

  def fleeLatched(unitId: Int, worstPenetrationPixels: Double): Boolean =
    fleeing(
      unitId,
      fires    = worstPenetrationPixels > 0,
      releases = worstPenetrationPixels < -fleeExitMarginPixels,
      initial  = worstPenetrationPixels > 0)

  /** Share of the cluster, nearest our base, counted as its rear. */
  val rearShare: Double = 0.33

  def rearOfCluster(members: Seq[Pixel], base: Pixel): Pixel = {
    if (members.isEmpty) return base

    val ordered = members.sortBy(_.tile.tileDistanceSquared(base.tile))
    val rearCount = Math.max(1, Math.ceil(ordered.size * rearShare).toInt)
    Maff.centroid(ordered.take(rearCount))
  }

  /** The closest enemy that can hit the Shuttle, if any. */
  def threatNearest(shuttle: FriendlyUnitInfo): Option[UnitInfo] =
    Maff.minBy(shuttle.matchups.threats.filter(t => t.canAttack(shuttle) && t.likelyStillThere))(_.pixelDistanceEdge(shuttle))
}
