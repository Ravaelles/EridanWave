package Placement.Pylons

import Placement.Access.PlaceLabels
import Placement.Access.PlacementQuery

/**
 * First Pylon: prefer the wall. A Wall-labeled Pylon fit outscores generic
 * Supply fits, so the opening Pylon joins the wall when one exists and
 * falls back to a Supply fit when it does not.
 */
object FirstPylonPlacement {

  def applyTo(query: PlacementQuery): Unit = {
    query.preferences.labelYes = Seq(PlaceLabels.Wall)
  }
}
