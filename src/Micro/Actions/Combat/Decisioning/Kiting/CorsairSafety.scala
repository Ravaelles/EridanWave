package Micro.Actions.Combat.Decisioning.Kiting

import Mathematics.Maff
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Corsair standoff distances against anything that can shoot back
 * (buildings and combat units alike).
 *
 * A fresh Corsair skims the edge of the threat at 1.8 tiles; a battered one
 * keeps up to 5 extra tiles, linearly with injury - 6.8 tiles fully wounded.
 * Other units keep the default 1-tile margin.
 *
 * Pure decisions on numbers so they stay unit-testable without a game;
 * Combat supplies the game-aware seam (the unit and its wounds).
 */
object CorsairSafety {

  /** Threat margin of a fresh Corsair, in tiles. */
  var baseMarginTiles: Double = 1.8

  /** Extra margin of a fully wounded Corsair, in tiles. Applied linearly with injury. */
  var woundBonusTiles: Double = 5.0

  def avoidanceMarginTiles(wound01: Double): Double =
    baseMarginTiles + woundBonusTiles * Maff.clamp(wound01, 0.0, 1.0)

  def avoidanceMarginPixels(unit: FriendlyUnitInfo): Double =
    if (Protoss.Corsair(unit)) avoidanceMarginTiles(unit.woundPercent()) * 32.0 else 32.0
}
