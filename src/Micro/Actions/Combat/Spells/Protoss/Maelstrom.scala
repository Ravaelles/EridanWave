package Micro.Actions.Combat.Spells.Protoss

import Micro.Actions.Combat.Spells.{SpellValue, TargetedSpell}
import ProxyBwapi.Races.Protoss
import ProxyBwapi.Techs.Tech
import ProxyBwapi.UnitClasses.UnitClass
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.?

object Maelstrom extends TargetedSpell {
  
  override protected def casterClass      : UnitClass = Protoss.DarkArchon
  override protected def tech             : Tech      = Protoss.Maelstrom
  override protected def aoe              : Boolean   = true
  override protected def castRangeTiles   : Int       = 10
  override protected def thresholdValue   : Double    = 1.0
  override protected def lookaheadPixels  : Int       = 6
  override protected def additionalConditions(unit: FriendlyUnitInfo): Boolean = unit.agent.shouldFight || unit.matchups.threatsInRange.nonEmpty

  val castFrames = 1

  override protected def valueTarget(target: UnitInfo, caster: FriendlyUnitInfo): Double = {
    if ( ! target.unitClass.canBeMaelstrommed) return 0.0
    if ( ! target.visible)  return 0.0
    // stasised/invincible ride SpellTargets.legal, which filters every
    // candidate before valueTarget runs - no second copy here.

    val multiplierConfidence  = SpellValue.confidence(caster.confidence11)
    val multiplierPlayer      = ?(target.isEnemy, 1.0, ?(target.isFriendly, -1.0, 0.0))
    val multiplierUnit        = target.unitClass.maelstromValue
    val multiplierDanger      = SpellValue.danger(caster.matchups.threatsInRange.nonEmpty, caster.unitClass.maxTotalHealth.toDouble, caster.totalHealth)
    val multiplierRich        = 0.5 + 0.5 * caster.team.map(_.maelstromCount).getOrElse(1)
    val output                = SpellValue.of(multiplierUnit, multiplierPlayer, multiplierConfidence, multiplierDanger, multiplierRich)
    output
  }
}
