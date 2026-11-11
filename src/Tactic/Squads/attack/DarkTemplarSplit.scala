package Tactic.Squads.attack

/**
 * Pure Dark Templar split (no game access): the first Templar always raids
 * in the independent squad - a lone bodyguard helps nobody, and an idle
 * first DT is the worst outcome. Past one, a single escort may stay with
 * a grown army (twenty warriors) while every other DT raids; smaller
 * armies leave every blade to the raiders so none sits squadless.
 */
object DarkTemplarSplit {

  /** Warriors that can shelter one Templar escort. */
  val armyForEscort: Int = 20

  def raidersToTake(totalDarkTemplar: Int, armyWarriors: Int): Int =
    if (totalDarkTemplar <= 0) 0
    else if (armyWarriors >= armyForEscort) Math.max(1, totalDarkTemplar - 1)
    else totalDarkTemplar
}
