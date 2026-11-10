package Micro.Actions.Combat.Decisioning.Formation

/**
 * Pure overwhelm rule (no game access): four times the local skim strength
 * means no formations. Dressing the line, holding slots and sliding in
 * formation all cost time; with overwhelming force the only correct move
 * is to attack.
 */
object OverwhelmLogic {

  /** Skip formations at this local skim ratio or better. */
  val overwhelmRatio: Double = 4.0

  def overwhelm(usSkim: Double, foeSkim: Double): Boolean =
    foeSkim <= 0 || usSkim / foeSkim >= overwhelmRatio
}
