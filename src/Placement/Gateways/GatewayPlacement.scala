package Placement.Gateways

import Placement.Access.PlaceLabels
import Placement.Access.PlacementQuery

/**
 * Standard Gateway: generic GroundProduction fit on any of our bases.
 * Verbatim move of the Gateway arm of PlacementQuery.resetDefaults; bare
 * get(n, Gateway) requests land here. Scoped variants stay with their
 * callers: per-base tech Gateways (PvT builds), proxy Gateways (Proxy4Gate
 * label) and the PvZFE main-base pair are deliberate specials, not the
 * standard case.
 */
object GatewayPlacement {

  def applyDefaults(query: PlacementQuery): Unit = {
    query.preferences.labelYes = query.preferences.labelYes :+ PlaceLabels.GroundProduction
  }
}
