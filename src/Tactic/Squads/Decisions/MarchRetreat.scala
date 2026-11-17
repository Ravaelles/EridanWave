package Tactic.Squads.Decisions

/**
 * Retreat outranks formation (no game access): a warrior the verdict
 * sent home walks home - no gate, no leg and no slot may hold it,
 * whatever their thresholds say. High priority beats low, unconditionally:
 * the march can never flap against a retreat it does not see, because it
 * never gets asked while the retreat is on. Explicit doctrines (assault,
 * toothless columns, roaming Dragoons) march first and keep their say;
 * Reavers keep their own brain. Everybody else that will not fight this
 * frame marches home.
 */
object MarchRetreat {

  def shouldMarchHome(
    isWarrior: Boolean,
    isReaver: Boolean,
    shouldFight: Boolean): Boolean =
    isWarrior && ! isReaver && ! shouldFight
}
