package Information.Geography

import Information.Geography.Types.Base
import Lifecycle.With
import Mathematics.Maff

/**
 * Total (non-throwing) accessors for well-known bases: one easy approach
 * instead of hunting throwing getters across call sites.
 *
 * [[Bases]] answers by ownership (our existing bases, if we hold any -
 * None on baseless maps). [[BaseLocations]] answers by map slot and
 * inference (our start slot, the enemy's inferred slots). Both return
 * Option: no bases, no crash.
 */
object Bases {

  /** Our existing main base, if we hold one. Total version of ourMain. */
  def main: Option[Base] = {
    val ours = With.geography.ourBases
    Maff.minBy(Maff.orElse(ours.filter(_.isMain), ours, With.geography.mains))(b =>
      b.heart.groundTiles(With.self.startTile) + b.heart.tileDistanceFast(With.self.startTile) / 1000.0)
  }

  /** Our existing natural base, if we hold one. Total version of ourNatural. */
  def natural: Option[Base] = {
    main.flatMap(_.natural.filter(_.naturalOf.exists(_.isOurs)))
      .orElse(With.geography.bases.find(_.naturalOf.exists(_.isOurs)))
      .orElse(main.flatMap(m =>
        Maff.minBy(With.geography.bases.filterNot(_ == m))(_.townHallTile.groundPixels(m.townHallTile))))
  }
}

object BaseLocations {

  /** The base sitting on our start location, whoever holds it now. */
  def main: Option[Base] = With.self.startTile.base

  /** Our start location's natural, if the map has one. */
  def natural: Option[Base] = main.flatMap(_.natural)

  /** The enemy's inferred main base, if we have a read on it. */
  def enemyMain: Option[Base] = With.scouting.enemyMain

  /** The enemy's inferred natural base, if we have a read on it. */
  def enemyNatural: Option[Base] = With.scouting.enemyNatural
}
