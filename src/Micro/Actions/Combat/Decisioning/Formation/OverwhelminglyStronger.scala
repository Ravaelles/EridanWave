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

  /**
   * The forecast variant: only a measured enemy outweighs dressing. A
   * forecast whose enemy side carries no strength yet (approach, no contact)
   * reads as four-to-one in locally() - exactly the march where the line is
   * formed - so zero measured strength never skips formations.
   */
  def measuredLocally(usSkim: Double, foeSkim: Double): Boolean =
    foeSkim > 0 && locally(usSkim, foeSkim)

  /**
   * No forecast means dress: unknown strength never skips, only a known
   * four-to-one does (unlike locally(0, 0), which is true in isolation).
   */
  def skipFormation(unit: FriendlyUnitInfo): Boolean =
    unit.combatForecast.exists(battle =>
      measuredLocally(battle.us.skimStrengthTotal, battle.enemy.skimStrengthTotal))
}
