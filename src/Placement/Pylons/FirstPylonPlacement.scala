package Placement.Pylons

import Lifecycle.With
import Placement.Access.PlaceLabels
import Placement.Access.PlacementQuery

/**
 * First Pylon: right next to the main hall so it finishes ASAP. A hard
 * main-zone filter (soft preferences lose to zone weight 5 and base
 * weight 9) plus hall tile preference pick the closest main Supply fit.
 * A Wall-labeled fit still wins the tiebreak when the wall sits inside.
 */
object FirstPylonPlacement {

  def applyTo(query: PlacementQuery): Unit = {
    query.preferences.labelYes = Seq(PlaceLabels.Wall)
    if (With.geography.ourBases.nonEmpty) {
      query.requirements.zone = Seq(With.geography.ourMain.zone)
      query.preferences.tile = Seq(With.geography.ourMain.townHallTile)
    }
  }
}
