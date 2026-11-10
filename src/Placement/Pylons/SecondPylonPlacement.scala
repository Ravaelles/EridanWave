package Placement.Pylons

import Lifecycle.With
import Placement.Access.PlaceLabels
import Placement.Access.PlacementQuery

/**
 * Second Pylon: power the main base tech cluster. Scoped to our main so
 * production and tech stay under one power grid; anywhere else still
 * wins when the main has no free Supply fit.
 */
object SecondPylonPlacement {

  def applyTo(query: PlacementQuery): Unit = {
    query.preferences.labelYes = Seq(PlaceLabels.Supply)
    query.preferences.base = With.geography.ourBases.filter(_.isOurMain)
  }
}
