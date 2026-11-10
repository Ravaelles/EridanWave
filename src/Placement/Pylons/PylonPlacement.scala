package Placement.Pylons

import Lifecycle.With
import Placement.Access.PlacementQuery
import ProxyBwapi.Races.Protoss

/**
 * Pylon placement by case, one class per case. Wired once in
 * PlacementQuery.resetDefaults, so every Pylon request flows through here
 * instead of sharing one generic Supply label. Covered elsewhere, not here:
 * cannon-rush Pylons (CannonRushFinder places them directly) and defense
 * Pylons (the defense query already scopes base and zone). Proxy Pylons are
 * the next case to add once a caller scopes them.
 */
object PylonPlacement {

  sealed trait PylonCase

  case object FirstPylon extends PylonCase

  case object SecondPylon extends PylonCase

  case object SupplyPylon extends PylonCase

  /** Which Pylon this is by live count. Pure, pinned by suite. */
  def caseFor(pylonCount: Int): PylonCase =
    if (pylonCount <= 0) FirstPylon
    else if (pylonCount == 1) SecondPylon
    else SupplyPylon

  def applyCase(query: PlacementQuery): Unit = {
    caseFor(With.units.countOurs(Protoss.Pylon)) match {
      case FirstPylon   => FirstPylonPlacement.applyTo(query)
      case SecondPylon  => SecondPylonPlacement.applyTo(query)
      case SupplyPylon  => SupplyPylonPlacement.applyTo(query)
    }
  }
}
