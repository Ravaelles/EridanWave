package Placement.Pylons

import Lifecycle.With
import Placement.Access.PlaceLabels
import Placement.Access.PlacementQuery

/**
 * Second Pylon: main supply inside the town hall's neighbourhood.
 * `preferTile(townHallTile)` used to point at a tile the Nexus already
 * occupies, so every Supply fit scored the same and template order dropped
 * the Pylon on the main choke, where Zealots had to defend it. The hard
 * near-hall radius makes proximity a requirement instead of a tiebreak.
 */
object SecondPylonPlacement {

  /** Pylon origin stays this close to the town hall, in tiles. */
  val maximumTownHallTiles: Int = 8

  def nearTownHall(distanceTiles: Int): Boolean =
    distanceTiles <= maximumTownHallTiles

  def applyTo(query: PlacementQuery): Unit = {
    query.preferences.labelYes = Seq(PlaceLabels.Supply)
    if (With.geography.ourBases.nonEmpty) {
      val main = With.geography.ourMain
      query.requirements.zone = Seq(main.zone)
      query.requireNear(main.townHallTile, maximumTownHallTiles)
    }
  }
}
