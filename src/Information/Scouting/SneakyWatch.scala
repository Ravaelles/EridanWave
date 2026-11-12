package Information.Scouting

import Debugging.CherryVis.CherryVis
import Lifecycle.With
import ProxyBwapi.Races.{Protoss, Terran, Zerg}

import scala.collection.mutable

/**
 * Sneaky-unit watch: the moment we SEE an enemy Lurker, Dark Templar,
 * Wraith or Ghost, the trace gets a global `WeDetected: X` line. Tech
 * inference (EnemyTechs) knows the research happened; this says the unit
 * itself is on the map and visible - the thing the owner actually wants
 * to see in the log while watching a game. Once per class per game.
 */
trait SneakyWatch {

  private val sneakyReported = mutable.HashSet[String]()

  protected def updateSneakyWatch(): Unit = {
    With.units.enemy.view
      .filter(e => e.alive && e.visible)
      .foreach(e => SneakyWatch.sneakyLabel(
        Zerg.Lurker(e),
        Protoss.DarkTemplar(e),
        Terran.Wraith(e),
        Terran.Ghost(e)).foreach(label => {
        if ( ! sneakyReported.contains(label)) {
          sneakyReported += label
          CherryVis.log("WeDetected: " + label)
        }
      }))
  }
}

object SneakyWatch {

  /** Trace label for a sneaky enemy, if it is one. Pure for the suite. */
  def sneakyLabel(isLurker: Boolean, isDarkTemplar: Boolean, isWraith: Boolean, isGhost: Boolean): Option[String] =
    if (isLurker) Some("Lurkers")
    else if (isDarkTemplar) Some("DarkTemplars")
    else if (isWraith) Some("Wraiths")
    else if (isGhost) Some("Ghosts")
    else None
}
