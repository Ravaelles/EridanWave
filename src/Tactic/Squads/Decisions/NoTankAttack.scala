package Tactic.Squads.Decisions

/**
 * Tankless-force gate (no game access): Dragoons attack a Terran with
 * no tanks instead of posing for guns that are not there. This used to
 * be a march gate (walk straight at the goal, skip the slots) - but
 * the intent was never about locomotion, it was about the fight: the
 * squad should attack. So the gate votes posture Fight now and leaves
 * the walk to the slots; siege lines keep the careful approach, Marines
 * and Medics do not outrange a Dragoon column enough to matter. The
 * caller checks the matchup and the tanks; this only combines them.
 */
object NoTankAttack {

  def shouldForceAttack(hasDragoon: Boolean, enemyIsTerran: Boolean, enemyHasTanks: Boolean): Boolean =
    hasDragoon && enemyIsTerran && ! enemyHasTanks
}
