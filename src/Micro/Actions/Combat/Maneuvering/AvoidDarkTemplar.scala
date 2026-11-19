package Micro.Actions.Combat.Maneuvering

import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Cloaked-Templar dodge (thin child of the shared back): a remembered
 * Dark Templar that cannot be seen kills in two swipes, so the line
 * steps around the memory instead of rallying or engaging through it.
 * Visible Templar are fought, not fled.
 */
object AvoidDarkTemplar {

  /** The blade kills at melee; the dodge starts one step outside that. */
  val avoidTiles: Double = 4.0

  /**
   * Workers give the blade extra room: they cannot answer back, so the
   * dodge starts wider instead of trading a probe for a late step out.
   */
  val workerAvoidTiles: Double = 5.5

  def avoidTilesFor(isWorker: Boolean): Double =
    if (isWorker) workerAvoidTiles else avoidTiles

  /** Escape stride, in tiles: straight out, no pathing detours. */
  val fleeTiles: Double = 10.0

  def act(unit: FriendlyUnitInfo): Boolean =
    AvoidUnseenKillers.avoid(unit, Protoss.DarkTemplar(_), avoidTilesFor(unit.unitClass.isWorker), fleeTiles, "AvoidDarkTemplar")
}
