package Micro.Targeting.FiltersRequired

import Lifecycle.{Enemy, With}
import Micro.Targeting.TargetFilter
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Scout discipline, PvP (required filter): our Scouts hunt Carriers only -
 * never Interceptors - and never alone. A wingmate inside five tiles or
 * the Carrier keeps its shields; lone Scouts hold with the line instead
 * of feeding it piecemeal.
 */
object TargetFilterScout extends TargetFilter {

  /** Wingmate range: a Scout engages a Carrier only with a mate this close. */
  val wingmateTiles: Double = 5.0

  /**
   * Victim vote (no game access): outside a Protoss mirror everything is
   * legal; inside it only Carriers with a wingmate near pass.
   */
  def allowsVictim(isScout: Boolean, enemyIsProtoss: Boolean, targetIsCarrier: Boolean, mateNear: Boolean): Boolean =
    ! (isScout && enemyIsProtoss) || (targetIsCarrier && mateNear)

  override def legal(attacker: FriendlyUnitInfo, target: UnitInfo): Boolean = {
    if ( ! Protoss.Scout(attacker) || ! Enemy.protoss()) return true

    allowsVictim(
      isScout         = true,
      enemyIsProtoss  = true,
      targetIsCarrier = Protoss.Carrier(target),
      mateNear        = hasWingmate(attacker))
  }

  def hasWingmate(scout: FriendlyUnitInfo): Boolean =
    With.units.ours.exists(o => o.id != scout.id && o.alive && Protoss.Scout(o) &&
      scout.tileDistanceCenter(o.pixel) <= wingmateTiles)
}
