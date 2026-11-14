package Micro.Actions.Combat.Maneuvering

import ProxyBwapi.Races.Zerg
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Burrowed-Lurker dodge (thin child of the shared back): a remembered
 * Lurker that cannot be seen cannot be shot, and its spines outrange
 * every ground duel - so the line steps around the memory instead of
 * rallying or engaging through it. Visible Lurkers are fought, not fled.
 */
object AvoidLurkers {

  /** Lurker spines reach six tiles; the dodge starts two outside that. */
  val avoidTiles: Double = 8.0

  /** Escape stride, in tiles: straight out, no pathing detours. */
  val fleeTiles: Double = 10.0

  def act(unit: FriendlyUnitInfo): Boolean =
    AvoidUnseenKillers.avoid(unit, Zerg.Lurker(_), avoidTiles, fleeTiles, "AvoidLurkers")
}
