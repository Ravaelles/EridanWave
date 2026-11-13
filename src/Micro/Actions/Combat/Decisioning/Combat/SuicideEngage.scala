package Micro.Actions.Combat.Decisioning

/**
 * Pure suicide-attack decision (no game access): a suicide unit that has
 * not started an attack for a long while charges the nearest enemy,
 * period - no range advantage or local superiority required. The generic
 * idle rule fires first when it is safe (150 frames); this is the
 * escalation for when it is not, so a duel that never becomes safe still
 * ends in a trade instead of a march. Frames are engine frames.
 */
object SuicideEngage {

  /** Force the attack past this many idle frames without a started attack. */
  val idleFrames: Int = 240 // 10 seconds

  def shouldForceAttack(framesSinceAttack: Int, hasEnemy: Boolean, isSuicideMission: Boolean): Boolean =
    isSuicideMission && hasEnemy && framesSinceAttack > idleFrames
}
