package Tactic.Squads.Decisions

/**
 * Pure tankless-march gate (no game access): Dragoons walk straight at a
 * Terran with no tanks instead of rallying to the pack centroid or
 * dressing the line. Siege lines force a careful approach; Marines and
 * Medics do not outrange a Dragoon column enough to matter, so the
 * slots and waits only trickle the line. The caller checks the matchup
 * and the tanks; this only combines them.
 */
object NoTankMarch {

  def shouldMarchDirectly(isDragoon: Boolean, enemyIsTerran: Boolean, enemyHasTanks: Boolean): Boolean =
    isDragoon && enemyIsTerran && ! enemyHasTanks
}
