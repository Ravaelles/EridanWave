package Placement.Pylons

import Lifecycle.With
import Placement.Access.PlaceLabels
import Placement.Access.PlacementQuery

/**
 * First Pylon, Stardust-style: main zone, edge toward the exit, off the
 * minerals but adjacent to the base. A hard main-zone filter keeps it out
 * of the choke (soft preferences lose to zone weight 5 and base weight 9);
 * preferring the zone exit picks the main Supply fit closest to the exit,
 * which is the edge fit, not the central one. No Wall label: the wall sits
 * forward and slow, and this Pylon is about speed.
 */
object FirstPylonPlacement {

  def applyTo(query: PlacementQuery): Unit = {
    query.preferences.labelYes = Seq(PlaceLabels.Supply)
    if (With.geography.ourBases.nonEmpty) {
      val main = With.geography.ourMain
      query.requirements.zone = Seq(main.zone)
      query.preferences.tile = Seq(main.zone.exitNowOrHeart)
    }
  }
}
