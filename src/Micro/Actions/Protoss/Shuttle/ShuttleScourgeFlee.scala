package Micro.Actions.Protoss.Shuttle

import Lifecycle.With
import Mathematics.Points.Pixel
import ProxyBwapi.Races.{Protoss, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import ProxyBwapi.UnitTracking.AliveEnemies

/**
 * Scourge flee for a loaded Shuttle: a transport carrying Reavers runs at
 * twelve tiles toward the rear of its cluster; at the generic three the
 * Scourge are already on it. Returns the rear pixel to run to, if running
 * applies; the caller issues the order.
 */
object ShuttleScourgeFlee {

  /**
   * Run only with Reavers aboard and Scourge inside the flee radius.
   * Pure, so the trigger can be argued with in a suite; the caller owns
   * the order and the destination.
   */
  def shouldRun(hasLoadedReaver: Boolean, scourgeWithinTiles: Boolean): Boolean =
    hasLoadedReaver && scourgeWithinTiles

  def fleePixel(shuttle: FriendlyUnitInfo): Option[Pixel] = {
    if ( ! Protoss.Shuttle(shuttle)) return None

    // Typed scan, not threats: Scourge carries no weapon in the data and
    // never lands in matchups. Engine pixels, threshold in tiles (rule 10).
    val scourgeNear = AliveEnemies.get().exists(u =>
      Zerg.Scourge(u) && u.likelyStillThere &&
        u.tileDistanceEdge(shuttle) < ShuttleFleeGeometry.scourgeFleeDist)
    if ( ! shouldRun(shuttle.loadedUnits.exists(Protoss.Reaver), scourgeNear)) return None

    val members = shuttle.squad.map(_.units.map(_.pixel))
      .getOrElse(shuttle.loadedUnits.map(_.pixel) :+ shuttle.pixel)

    Some(ShuttleFleeGeometry.rearOfCluster(
      members,
      With.geography.ourMain.heart.center))
  }
}
