package Tactic

import Information.Geography.Chokes
import Information.Geography.Types.Base
import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.Pixel
import ProxyBwapi.UnitInfo.UnitInfo
import Utilities.UnitFilters.IsWarrior

/**
 * The single readable place answering where the army should be.
 *
 * Priority, highest first:
 * 1. Breach: an owned base with enemy warriors in its zone - return to
 *    where the landing is (drops and Mutalisks over main included).
 * 2. Natural: once we decided to take it (a second town hall stands),
 *    poise at its choke. Holding main while the natural burns is how
 *    games are thrown away.
 * 3. Main: the default muster point before any expansion.
 */
object ArmyPosition {

  /** Our bases with enemy warriors standing in their zone, most threatened first. */
  def breachedBases: Seq[Base] =
    With.geography.ourBases
      .map(base => (base, invadersAt(base)))
      .filter(_._2.nonEmpty)
      .sortBy(-_._2.size)
      .map(_._1)

  /** Enemy warriors inside the base's zone. */
  def invadersAt(base: Base): Seq[UnitInfo] =
    With.units.enemy.filter(e => IsWarrior(e) && e.zone == base.zone).toSeq

  /** Our natural counts as taken once its town hall is ours, warping or done. */
  def naturalTaken: Boolean =
    With.geography.ourNatural.townHall.exists(_.isOurs)

  /** Where the landing is: centroid of the invaders, base heart as fallback. */
  def breachPosition(base: Base): Pixel = {
    val invaders = invadersAt(base)
    if (invaders.isEmpty) {
      base.heart.center
    } else {
      Pixel(
        invaders.view.map(_.pixel.x).sum / invaders.size,
        invaders.view.map(_.pixel.y).sum / invaders.size)
    }
  }

  /** Strongest breach pixel, if any owned base is invaded. */
  def breachPixel: Option[Pixel] =
    Maff.maxBy(breachedBases)(invadersAt(_).size).map(breachPosition)

  /** Full priority: breach, then natural choke, then main heart. */
  def stagingPosition: Pixel =
    breachPixel.getOrElse(naturalStaging.getOrElse(With.geography.ourMain.heart.center))

  /** Natural choke to poise at, when the natural is taken. */
  def naturalStaging: Option[Pixel] =
    if (naturalTaken) Some(Chokes.naturalChoke.map(_.pixelCenter).getOrElse(naturalHeart)) else None

  private def naturalHeart: Pixel =
    With.geography.ourNatural.heart.center

  /**
   * Drop-in for disengage routing (e.g. defense travel goals): keeps the
   * current point unless the priority says elsewhere. Breach always wins;
   * a taken natural pulls staging forward to its choke.
   */
  def fallback(current: Pixel): Pixel =
    breachPixel.orElse(if (naturalTaken) naturalStaging else None).getOrElse(current)
}
