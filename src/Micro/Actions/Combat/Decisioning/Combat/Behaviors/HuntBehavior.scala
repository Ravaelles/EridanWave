package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Target-less hunt: a warrior with fight consensus but no combat target
 * attack-moves toward the nearest known hittable enemy instead of posing
 * in formation slots until the timeout. Without a battle cluster there are
 * no matchups, so Target.choose stays empty and the line dresses forever -
 * the UMS duels froze a thousand pixels from passive hulls this way.
 * UMS duels only: no economy and static vicinities, so hunting is always
 * right; macro columns ride the march and must not chase remembered
 * contacts. Returns true when it moved the unit. Reavers keep their pair
 * doctrine (BeReaver owns their movement).
 */
object HuntBehavior {

  def run(unit: FriendlyUnitInfo): Boolean = {
    if ( ! shouldHunt(
      umsMode   = With.umsMode,
      isWarrior = unit.unitClass.isWarrior,
      isReaver  = Protoss.Reaver(unit),
      canMove   = unit.canMove,
      hasTarget = unit.agent.toAttack.isDefined)) return false

    val foe = Maff.minBy(huntables(unit))(unit.pixelDistanceCenter(_))
    if (foe.isEmpty) return false

    unit.agent.act("Hunt")
    Commander.attackMove(unit, foe.get.pixel)

    true
  }

  def huntables(unit: FriendlyUnitInfo): Seq[UnitInfo] =
    With.units.enemy.view.filter(e =>
      e.alive
      && e.likelyStillThere
      && unit.canAttack(e)).toSeq

  /**
   * Hunt gate (pure booleans for the suite): dueling walkers with no
   * target hunt. Reavers keep the pair doctrine; immobile and loaded
   * hulls never reach here (canMove).
   */
  def shouldHunt(
    umsMode   : Boolean,
    isWarrior : Boolean,
    isReaver  : Boolean,
    canMove   : Boolean,
    hasTarget : Boolean): Boolean =
    umsMode && isWarrior && ! isReaver && canMove && ! hasTarget
}
