package Micro.Actions.Combat.Maneuvering

import ProxyBwapi.Races.{Protoss, Zerg}
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

  /**
   * Scarab-loaded Reavers close to this instead of dodging: spines stop
   * at six tiles and the Reaver outranges them, so a hull with ammunition
   * walks to firing distance instead of milling outside. Below one
   * scarab the full dodge stands - an empty hull trades for nothing.
   */
  val reaverAvoidTiles: Double = 7.05

  def avoidTilesFor(isReaver: Boolean, scarabs: Int): Double =
    if (isReaver && scarabs > 0) reaverAvoidTiles else avoidTiles

  /** Escape stride, in tiles: straight out, no pathing detours. */
  val fleeTiles: Double = 10.0

  def act(unit: FriendlyUnitInfo): Boolean =
    AvoidUnseenKillers.avoid(
      unit,
      Zerg.Lurker(_),
      avoidTilesFor(Protoss.Reaver(unit), unit.scarabs),
      fleeTiles,
      "AvoidLurkers")
}
