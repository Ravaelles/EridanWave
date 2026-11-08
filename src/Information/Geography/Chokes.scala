package Information.Geography

import Information.Geography.Types.Edge
import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.{Pixel, Points}

/**
 * Total (non-throwing) accessors for well-known chokes, same easy approach
 * as Bases: one place, Option out, no bases-or-chokes assumptions.
 *
 * middleChoke is the odd one out on purpose: the most central lane that is
 * none of the base chokes, where the army can set up in formation and wait
 * for the enemy. That is neither a flee (disengage home) nor a fight
 * (engage the enemy) - a future middle-focus stance would march here and
 * hold, letting DressLine/FormationHold govern the wait.
 */
object Chokes {

  /** The choke out of our main base, if we hold one. */
  def mainChoke: Option[Edge] = Bases.main.flatMap(_.zone.exitOriginal)

  /** The choke out of our natural base, if we hold one. */
  def naturalChoke: Option[Edge] = Bases.natural.flatMap(_.zone.exitOriginal)

  /** The choke out of the enemy's inferred main, if we have a read on it. */
  def enemyMainChoke: Option[Edge] = BaseLocations.enemyMain.flatMap(_.zone.exitOriginal)

  /** The choke out of the enemy's inferred natural, if we have a read on it. */
  def enemyNaturalChoke: Option[Edge] = BaseLocations.enemyNatural.flatMap(_.zone.exitOriginal)

  /**
   * The most central lane that is none of the base chokes above.
   * Scored by distance to map middle minus choke radius: central and wide
   * enough to set up a formation around wins.
   */
  def middleChoke: Option[Edge] = {
    val taken = Set(mainChoke, naturalChoke, enemyMainChoke, enemyNaturalChoke).flatten.map(_.pixelCenter)
    Maff.minBy(With.geography.edges.filterNot(edge => taken.contains(edge.pixelCenter)))(middleScore)
  }

  /** Lower is more desirable. Pure numbers so the tradeoff stays testable. */
  def middleScoreOf(distancePixels: Double, radiusPixels: Double): Double =
    distancePixels - radiusPixels

  def middleScore(edge: Edge): Double =
    middleScoreOf(edge.pixelCenter.pixelDistance(Points.middle), edge.radiusPixels)
}
