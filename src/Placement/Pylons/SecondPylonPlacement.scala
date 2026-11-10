package Placement.Pylons

import Lifecycle.With
import Placement.Access.PlaceLabels
import Placement.Access.PlacementQuery

/**
 * Second Pylon: safe inside the main, never forward on the choke where
 * Zerglings eat it. The main-zone requirement excludes choke fits (chokes
 * separate zones by design); the hall tile preference keeps it central.
 */
object SecondPylonPlacement {

  def applyTo(query: PlacementQuery): Unit = {
    query.preferences.labelYes = Seq(PlaceLabels.Supply)
    if (With.geography.ourBases.nonEmpty) {
      query.requirements.zone = Seq(With.geography.ourMain.zone)
      query.preferences.tile = Seq(With.geography.ourMain.townHallTile)
    }
  }
}
