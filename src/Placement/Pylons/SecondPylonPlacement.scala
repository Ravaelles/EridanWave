package Placement.Pylons

import Lifecycle.With
import Placement.Access.PlaceLabels
import Placement.Access.PlacementQuery

/**
 * Second Pylon: generic main supply, where the first Pylon used to land.
 * Hard main-zone scope keeps it off the choke where Zerglings eat it; the
 * hall tile preference keeps it central to power the tech cluster.
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
