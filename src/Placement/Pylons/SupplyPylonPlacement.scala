package Placement.Pylons

import Placement.Access.PlaceLabels
import Placement.Access.PlacementQuery

/**
 * Late Pylons: generic supply anywhere, today's behavior preserved exactly
 * so the dispatcher changes nothing past the second Pylon.
 */
object SupplyPylonPlacement {

  def applyTo(query: PlacementQuery): Unit = {
    query.preferences.labelYes = Seq(PlaceLabels.Supply)
  }
}
