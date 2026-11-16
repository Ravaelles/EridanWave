package Tactic.Squads.support

import Utilities.Time.Seconds

/**
 * Expansion scout demand (pure numbers, no game access): from eighty
 * supply a Dragoon keeps the enemy third and fourth scouted, revisiting
 * each at least every minute. Workers-only contacts never hold the
 * scout - the pair doctrine already proved that waiting on blind
 * guesses mills the game away.
 */
object ExpansionScout {

  /** From this supply (200-scale) a Dragoon scouts the expansions. */
  val dragoonScoutSupply200: Int = 80

  /** Revisit a scouted expansion at least this often, in frames. */
  val revisitFrames: Int = Seconds(60)()

  def wantsDragoonScout(supplyUsed200: Int): Boolean =
    supplyUsed200 >= dragoonScoutSupply200
}
