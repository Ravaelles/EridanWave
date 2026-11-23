package Micro.Actions.Combat.Decisioning.Formation

import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * The one decision point for overwhelming force: two-and-a-half times
 * the local skim strength AND four times the global skim means no
 * formations anywhere. Dressing the line, holding slots and sliding in
 * formation all cost time the overwhelming force should spend attacking.
 * Either side below its bar still dresses: a local edge means nothing
 * against the reinforcements marching in, and a global edge means
 * nothing to the outnumbered hulls in contact. Pure numbers stay
 * testable; the unit seam below is the only game-aware entry, reused
 * wherever outnumbering decides behavior.
 */
object OverwhelminglyStronger {

  /** Skip formations at this local skim ratio or better. */
  val overwhelmLocalRatio: Double = 2.5

  /** ...and this global skim ratio or better. Both must hold. */
  val overwhelmGlobalRatio: Double = 4.0

  def locally(usSkim: Double, foeSkim: Double): Boolean =
    foeSkim <= 0 || usSkim / foeSkim >= overwhelmLocalRatio

  /**
   * Globally overwhelming (pure numbers for the suite): four-to-one with
   * a known enemy. An unknown enemy never overwhelms - at spawn the enemy
   * total reads zero, and that is exactly the march where the line forms.
   */
  def globally(usSkim: Double, foeSkim: Double): Boolean =
    foeSkim > 0 && usSkim / foeSkim >= overwhelmGlobalRatio

  /**
   * The forecast variant: only a measured enemy outweighs dressing. A
   * forecast whose enemy side carries no strength yet (approach, no contact)
   * reads as four-to-one in locally() - exactly the march where the line is
   * formed - so zero measured strength never skips formations.
   */
  def measuredLocally(usSkim: Double, foeSkim: Double): Boolean =
    foeSkim > 0 && locally(usSkim, foeSkim)

  /**
   * No forecast means dress: unknown strength never skips, only known
   * odds do (unlike locally(0, 0), which is true in isolation).
   * Global odds ride the world total: the local edge alone never skips
   * while the enemy masses elsewhere.
   */
  def skipFormation(unit: FriendlyUnitInfo): Boolean = {
    val local = unit.combatForecast.exists(battle =>
      measuredLocally(battle.us.skimStrengthTotal, battle.enemy.skimStrengthTotal))
    local && globally(
      Lifecycle.With.battles.globalAttack.us.skimStrengthTotal,
      Lifecycle.With.battles.globalAttack.enemy.skimStrengthTotal)
  }
}
