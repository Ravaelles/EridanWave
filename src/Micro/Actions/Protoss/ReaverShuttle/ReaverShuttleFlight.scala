package Micro.Actions.Protoss.ReaverShuttle

import Mathematics.Maff
import Mathematics.Points.Pixel
import Micro.Actions.Combat.Maneuvering.RangedReach
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
    Maff.woundBonus(woundPercent, 0.0, woundFleeBonusTiles)

  /** Loaded Shuttle margin over the threat, in pixels: skate fresh, run hurt. */
  def safetyMarginPixels(woundPercent: Double, fasterFlyer: Boolean): Double =
    (if (fasterFlyer) 64 else 160) + woundBonusTiles(woundPercent) * 32.0

  /**
   * Preemptive flee trigger (pure pixels): a ranged threat whose reach
   * plus the wound-scaled band touches us turns us around before the pack
   * closes - Hydralisks walking up to a Shuttle sitting in the enemy main
   * is how Shuttles die stupidly.
   */
  def threatensPreemptively(distPixels: Double, rangePixels: Double, marginPixels: Double): Boolean =
    RangedReach.isRanged(rangePixels) && distPixels < rangePixels + marginPixels

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
