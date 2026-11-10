package Micro.Actions.Combat.Decisioning.Formation

import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * The one decision point for overwhelming force: four times the local skim
 * strength means no formations anywhere. Dressing the line, holding slots
 * and sliding in formation all cost time the overwhelming force should
 * spend attacking. Pure numbers stay testable; the unit seam below is the
 * only game-aware entry, reused wherever outnumbering decides behavior.
 */
object OverwhelminglyStronger {

  /** Skip formations at this local skim ratio or better. */
  val overwhelmRatio: Double = 4.0

  def locally(usSkim: Double, foeSkim: Double): Boolean =
    foeSkim <= 0 || usSkim / foeSkim >= overwhelmRatio

  def skipFormation(unit: FriendlyUnitInfo): Boolean =
    unit.combatForecast.exists(battle =>
      locally(battle.us.skimStrengthTotal, battle.enemy.skimStrengthTotal))
}
