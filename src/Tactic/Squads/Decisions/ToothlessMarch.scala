package Tactic.Squads.Decisions

/**
 * Pure toothless-march gate (no game access): combat units march straight
 * at a Terran with neither Vultures nor Tanks instead of dressing,
 * rallying, slotting or returning - there is no siege line to respect
 * and no fast raiders to answer, so every formation dance only trickles
 * the line. The caller checks the matchup and the arsenal; this only
 * combines them.
 */
object ToothlessMarch {

  def shouldMarchDirectly(isCombat: Boolean, enemyIsTerran: Boolean, enemyHasTanks: Boolean, enemyHasVultures: Boolean): Boolean =
    isCombat && enemyIsTerran && ! enemyHasTanks && ! enemyHasVultures
}
