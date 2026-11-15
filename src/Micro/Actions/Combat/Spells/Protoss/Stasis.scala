package Micro.Actions.Combat.Spells.Protoss

import Lifecycle.With
import Micro.Actions.Combat.Spells.{SpellValue, TargetedSpell}
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.Techs.Tech
import ProxyBwapi.UnitClasses.UnitClass
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

object Stasis extends TargetedSpell {
  
  override protected def casterClass    : UnitClass = Protoss.Arbiter
  override protected def tech           : Tech      = Protoss.Stasis
  override protected def aoe            : Boolean   = true
  override protected def castRangeTiles : Int       = 9
  override protected def thresholdValue : Double    = casterClass.subjectiveValue
  override protected def additionalConditions(unit: FriendlyUnitInfo): Boolean = unit.agent.shouldFight
  
  override protected def valueTarget(target: UnitInfo, caster: FriendlyUnitInfo): Double = {
    if (With.grids.psionicStorm.isSet(target.tile)) return 0.0
    if (target.unitClass.isSpell)       return 0.0
    if (target.unitClass.isBuilding) return 0.0
    if (target.underStorm) return 0.0
    // stasised/invincible ride SpellTargets.legal, which filters every
    // candidate before valueTarget runs - no second copy here.

    if (target.isAny(
      Protoss.Interceptor,
      Zerg.Larva,
      Zerg.Egg,
      Zerg.LurkerEgg)) return 0.0

    val teamValue = (
      SpellValue.crowd(target.matchups.targets.size)
      * (if (target.team.exists(_.centroidAir.base.exists(_.owner.isEnemy))
        && ! target.unitClass.isWorker
        && target.matchups.targetsInRange.isEmpty) 0.3 else 1.0)
      * (if (target.isFriendly)
          -2.0
        else if (target.isEnemy) (
          1.0
          + (if (target.unitClass.isDetector) 2.0 else 0.0)
          + (if (target.is(Terran.SiegeTankSieged)) 0.5 else if (target.is(Terran.SiegeTankUnsieged)) 0.25 else 0.0)
        )
      else 0.0))
    val fightValue = if (caster.agent.shouldFight) 1.0 else 0.25
    
    val output = target.subjectiveValue * teamValue * fightValue
    output
  }
}

