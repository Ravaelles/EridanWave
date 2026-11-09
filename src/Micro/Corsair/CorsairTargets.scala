package Micro.Corsair

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.Pixel
import ProxyBwapi.UnitInfo.UnitInfo

/**
 * Shared target picking for Corsair hunters: live enemies of a kind,
 * then closest-wounded-first inside the close radius. One shape instead
 * of a copy per behavior.
 */
object CorsairTargets {

  val CloseRadiusSq: Double = 6 * 6 * 32 * 32

  def liveEnemiesOf(filter: UnitInfo => Boolean): Seq[UnitInfo] =
    With.units.enemy.filter(e => filter(e) && e.likelyStillThere).toSeq

  def closestWoundedFirst(units: Seq[UnitInfo], from: Pixel): Option[UnitInfo] =
    Maff.minBy(units)(u => (-u.woundPercent(), u.pixelDistanceSquared(from)))
}
