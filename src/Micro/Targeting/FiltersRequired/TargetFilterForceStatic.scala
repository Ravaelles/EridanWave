package Micro.Targeting.FiltersRequired

import Information.Battles.ArmyStrength
import Lifecycle.With
import Micro.Targeting.TargetFilter
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Tactic.ForceGlobalAttack

/**
 * Forced-attack static discipline (required filter): while the unseen
 * blade forces the global attack, covered static defense is not an
 * objective without advantage - the army walks at units, workers and
 * bases instead of chewing a cannon line even. Free shots inside our own
 * range stay legal, and a static already hitting us answers itself.
 * Outside the force everything passes through untouched.
 */
object TargetFilterForceStatic extends TargetFilter {

  private var frame: Int = -1
  private var forceActive: Boolean = false
  private var advantaged: Boolean = false

  override def legal(attacker: FriendlyUnitInfo, target: UnitInfo): Boolean = {
    if ( ! target.unitClass.isStaticDefense) return true

    refresh()
    if ( ! forceActive) return true

    ForceGlobalAttack.mayHitStatic(
      forceActive             = true,
      targetIsStaticDefense   = true,
      advantaged              = advantaged,
      inOurRange              = attacker.inRangeToAttack(target))
  }

  private def refresh(): Unit = {
    if (frame == With.frame) return

    frame = With.frame
    forceActive = ForceGlobalAttack.shouldForce(
      enemyIsProtoss     = With.enemies.exists(_.isProtoss),
      enemyIsTerran      = With.enemies.exists(_.isTerran),
      haveDarkTemplar    = With.units.ours.exists(o => o.alive && Protoss.DarkTemplar(o)),
      enemyHasDetection  = With.units.enemy.exists(e => e.alive && e.isAny(Protoss.Observer, Terran.ScienceVessel)),
      macroGame          = ! With.umsMode)
    advantaged = ArmyStrength.score(
      With.battles.globalAttack.us.skimStrengthUnits,
      With.battles.globalAttack.enemy.skimStrengthUnits) >= ForceGlobalAttack.staticAttackScore
  }
}
