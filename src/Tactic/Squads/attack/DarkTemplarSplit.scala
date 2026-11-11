package Tactic.Squads.attack

/**
 * Pure Dark Templar split (no game access): the first Templar always raids
 * in the independent squad - a lone bodyguard helps nobody, and an idle
 * first DT is the worst outcome. Past one, a single bodyguard stays with
 * the main army while every other DT raids.
 */
object DarkTemplarSplit {

  def raidersToTake(totalDarkTemplar: Int): Int =
    if (totalDarkTemplar <= 0) 0 else Math.max(1, totalDarkTemplar - 1)
}
