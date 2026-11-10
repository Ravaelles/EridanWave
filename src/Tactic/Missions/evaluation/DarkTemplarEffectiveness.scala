package Tactic.Missions.evaluation

import ProxyBwapi.Races.Protoss

/**
 * Dark Templar production policy, the reference adapter for
 * EffectivenessGate: at least two Templars always (the army guard plus
 * the distractor), never more than seven alive, last-two profitability
 * otherwise, gas flood at four hundred unless lifetime runs negative.
 * Thin seam over the KillCredits ledger; the next unit class copies
 * this shape with its own params.
 */
object DarkTemplarEffectiveness {

  val params = EffectivenessGate.EffectivenessParams(
    minAlways    = 2,
    maxAlive     = 7,
    recentWindow = 2,
    gasFlood     = 400)

  def allows(produced: Int, alive: Int, gas: Int): Boolean =
    EffectivenessGate.allows(
      produced,
      alive,
      KillCredits.recentAverageOf(Protoss.DarkTemplar, params.recentWindow),
      KillCredits.classAverageOf(Protoss.DarkTemplar),
      gas,
      params)
}
