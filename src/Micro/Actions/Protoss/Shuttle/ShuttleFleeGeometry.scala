package Micro.Actions.Protoss.Shuttle

import Mathematics.Maff
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

  /** Wounds scale the threat margin tenfold: a fresh Shuttle may skate
    * threats it can outrun, a bleeding one runs early. Mirrors the
    * MatchupAnalysis safety branches (64 skating, 160 standing). */
  val woundMarginMult: Double = 10.0

  def safetyMarginPixels(woundPercent: Double, fasterFlyer: Boolean): Double =
    ?(fasterFlyer, 64, 160) * (1 + woundMarginMult * woundPercent)

  /** Share of the cluster, nearest our base, counted as its rear. */
  val rearShare: Double = 0.33

  def rearOfCluster(members: Seq[Pixel], base: Pixel): Pixel = {
    if (members.isEmpty) return base

    val ordered = members.sortBy(_.tile.tileDistanceSquared(base.tile))
    val rearCount = Math.max(1, Math.ceil(ordered.size * rearShare).toInt)
    Maff.centroid(ordered.take(rearCount))
  }
}
