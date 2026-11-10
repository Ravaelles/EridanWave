package Tactic.Squads.attack

/**
 * Pure Dark Templar split (no game access): one DT always stays with the
 * main army - a cloaked bodyguard that costs no extra thought - and
 * every other DT raids. Zero or one Templar means zero raiders.
 */
object DarkTemplarSplit {

  def raidersToTake(totalDarkTemplar: Int): Int =
    Math.max(0, totalDarkTemplar - 1)
}
