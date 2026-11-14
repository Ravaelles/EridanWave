package Placement.Pylons

import Lifecycle.With
import Placement.Access.PlacementQuery
import ProxyBwapi.Races.Protoss

/**
 * Pylon placement by case, one class per case. Wired once in
 * PlacementQuery.resetDefaults, so every default Pylon request flows
 * through here instead of sharing one generic Supply label. First is
 * Stardust-style (main edge toward the exit), second is central main
 * supply, the rest are generic supply. Covered elsewhere, not here:
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
      case SupplyPylon  => SupplyPylonPlacement.applyTo(query); hugNatural(query)
    }
    keepOffChokes(query)
  }

  /** A Pylon this close to our natural is at the natural, in tiles. */
  val naturalReachTiles: Int = 10

  /** At most this many Pylons at the natural: the wall owns the rest. */
  val maxNaturalPylons: Int = 1

  /**
   * Supply Pylons at the natural hug the hall: inside the reach only a
   * foundation touching the hall is offered, so a loose Pylon cannot eat
   * the wall's tiles or sit powering nothing we planned - and that holds
   * before the hall itself exists. Once the one natural Pylon is up, the
   * whole reach is banned: a second loose Pylon there is the same mistake
   * with a building already on it.
   */
  private def hugNatural(query: PlacementQuery): Unit = {
    if (With.geography.ourBases.isEmpty) return

    val natural = With.geography.ourNatural
    val pylonsAtNatural = With.units.ours.count(u =>
      Protoss.Pylon(u) && u.pixelDistanceEdge(natural.heart.center) * Mathematics.Maff.inv32 <= naturalReachTiles)
    if (pylonsAtNatural >= maxNaturalPylons) {
      query.requireFar(natural.townHallTile, naturalReachTiles)
    } else {
      query.requireNearAdjacent(natural.townHallTile, Protoss.Nexus.tileWidth, Protoss.Nexus.tileHeight, naturalReachTiles)
    }
  }

  /** Nearest Pylon origin stays this far from any choke center, in tiles. */
  val minimumChokeTiles: Int = 4

  def farFromChoke(distanceTiles: Int): Boolean =
    distanceTiles >= minimumChokeTiles

  /**
   * No base Pylon on a choke: every choke center on the map bans its
   * Chebyshev disc, so Zealots never defend a Supply Pylon in the pass.
   * Walls and cannon rushes place their own Pylons and are unaffected.
   */
  private def keepOffChokes(query: PlacementQuery): Unit = {
    With.geography.metros.view
      .flatMap(_.exits)
      .map(_.pixelCenter.walkableTile)
      .foreach(choke => query.requireFar(choke, minimumChokeTiles))
  }
}
