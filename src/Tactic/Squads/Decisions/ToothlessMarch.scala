package Tactic.Squads.Decisions

/**
 * Pure toothless-attack vote (no game access): our warriors attack a
 * Terran with neither Vultures nor Tanks instead of dressing against
 * them - there is no siege line to respect and no fast raiders to
 * answer, so every formation dance only trickles the line. The caller checks the matchup and the arsenal; this only
 * combines them.
 */
object ToothlessMarch {

  def shouldForceAttack(hasWarrior: Boolean, enemyIsTerran: Boolean, enemyHasTanks: Boolean, enemyHasVultures: Boolean): Boolean =
    hasWarrior && enemyIsTerran && ! enemyHasTanks && ! enemyHasVultures
}
