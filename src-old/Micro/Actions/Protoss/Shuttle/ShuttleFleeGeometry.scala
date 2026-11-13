package Micro.Actions.Protoss.Shuttle

import Mathematics.Maff
import Micro.Actions.Combat.Maneuvering.RangedReach
import Mathematics.Points.Pixel
import Utilities.?

/**
 * Pure flee geometry for a loaded Shuttle (no game access): Scourge
 * outrun a Shuttle that dithers, so a transport carrying Reavers runs
 * early - twelve tiles, not three - toward the rear of its cluster.
 * The rear is the centroid of the third of the cluster nearest our
 * base, never the median: the middle may be mid-fight, the rear is the
 * way out. Pure pixels for the suite; the caller gathers members.
 */
object ShuttleFleeGeometry {

  /** Scourge this close, in tiles, send a loaded Shuttle running. */
  val scourgeFleeDist: Double = 12.0

  /** Share of the cluster, nearest our base, counted as its rear. */
  val rearShare: Double = 0.33

  /**
   * The one wound curve for the whole Shuttle, in tiles: a fresh hull
   * skates what it can outrun, a bleeding one runs early. The unsafe
   * gate adds it to its flat base margin below; the preemptive band in
   * BeShuttle adds it to its own base the same way - one knob retunes
   * both. Mirrors the MatchupAnalysis safety branches (64 skating,
   * 160 standing).
   */
  val woundFleeBonusTiles: Double = 9.0

  def woundBonusTiles(woundPercent: Double): Double =
    Maff.clamp01(woundPercent) * woundFleeBonusTiles

  def safetyMarginPixels(woundPercent: Double, fasterFlyer: Boolean): Double =
    ?(fasterFlyer, 64, 160) + woundBonusTiles(woundPercent) * 32.0

  /** Preemptive band beyond a ranged threat's own reach, in tiles. */
  val baseFleeMarginTiles: Double = 4.0

  /**
   * Preemptive flee trigger (pure pixels for the suite): a ranged threat
   * whose reach plus the wound-scaled band touches us turns the Shuttle
   * around before the pack closes - the flat twelve-tile radius alone
   * lets Hydralisks walk up to a Shuttle sitting in the enemy main.
   */
  def threatensPreemptively(distPixels: Double, rangePixels: Double, marginPixels: Double): Boolean =
    RangedReach.isRanged(rangePixels) && distPixels < rangePixels + marginPixels

  def rearOfCluster(members: Seq[Pixel], base: Pixel): Pixel = {
    if (members.isEmpty) return base

    val ordered = members.sortBy(_.tile.tileDistanceSquared(base.tile))
    val rearCount = Math.max(1, Math.ceil(ordered.size * rearShare).toInt)
    Maff.centroid(ordered.take(rearCount))
  }
}
