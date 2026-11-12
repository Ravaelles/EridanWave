package Micro.Actions.Combat.Maneuvering

import Mathematics.Maff
import Mathematics.Points.Pixel

/**
 * One withdrawing rule: rally to the safest ally further from threats than
 * we are, else fall back home. Pure pixels and distances, so the rally pick
 * can be argued with in a test instead of in a rush.
 *
 * Note: this covers the Zealot rush rally only. The Dragoon slowlot retreat
 * is waypoint-based and the generic Retreat plans around force vectors;
 * unifying those would change behavior, so each keeps its own rule.
 */
object Withdraw {

  /** Allies need this much more threat distance than us to count as safer. */
  val saferMarginPixels: Double = 64.0

  def toAllies(
    ownThreatPixels : Option[Double],
    allies          : Seq[(Pixel, Double)],
    home            : Pixel,
    marginPixels    : Double = saferMarginPixels): Pixel = {
    val safer = allies.filter({ case (_, dist) => ownThreatPixels.exists(own => dist > own + marginPixels) })

    Maff.minBy(safer)(_._2).map(_._1).getOrElse(home)
  }
}
