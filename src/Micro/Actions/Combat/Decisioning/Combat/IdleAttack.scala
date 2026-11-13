package Micro.Actions.Combat.Decisioning

/**
 * Pure idle-attack decision (no game access): a combat unit that has not
 * started an attack for a long while, holds a hittable enemy in range and
 * is locally ahead stops dithering and shoots it. Only for units that
 * want to fight (never fleeing or retreating) - forcing fire into a lost
 * position feeds the enemy. The caller reads recency, range and posture
 * from the unit; this only combines them. Frames are engine frames.
 */
object IdleAttack {

  /** Idle past this many frames since the last started attack, force fire. */
  val idleFrames: Int = 150

  def shouldForceAttack(
    framesSinceAttack: Int,
    hasHittableEnemy: Boolean,
    shouldFight: Boolean,
    locallyAhead: Boolean): Boolean =
    shouldFight && hasHittableEnemy && locallyAhead && framesSinceAttack > idleFrames
}
