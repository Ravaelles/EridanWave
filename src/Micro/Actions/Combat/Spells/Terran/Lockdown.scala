package Micro.Actions.Combat.Spells.Terran

import Micro.Actions.Combat.Spells.TargetedSpell
import ProxyBwapi.Races.Terran
import ProxyBwapi.Techs.Tech
import ProxyBwapi.UnitClasses.UnitClass
import ProxyBwapi.UnitInfo._

object Lockdown extends TargetedSpell {

  /** Lockdown: freeze the most valuable mechanical threat at Ghost range; same reach pricing as Yamato, only the target filter differs. */
  override protected def casterClass    : UnitClass = Terran.Ghost
  override protected def tech           : Tech      = Terran.Lockdown
  override protected def aoe            : Boolean   = false
  override protected def castRangeTiles : Int       = 8
  override protected def thresholdValue : Double    = Terran.Goliath.subjectiveValue

  override protected def valueTarget(target: UnitInfo, caster: FriendlyUnitInfo): Double = {
    if ( ! target.isEnemy)                return 0.0
    if ( ! target.unitClass.isMechanical) return 0.0
    if (target.unitClass.isBuilding)      return 0.0
    if (target.lockedDown)                return 0.0

    TargetedSpell.reachValue(target.subjectiveValue, caster.pixelDistanceEdge(target) - 32 * castRangeTiles, caster.matchups.pixelsEntangled)
  }
}
