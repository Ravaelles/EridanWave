package Tactic.Squads.Decisions

/**
 * Slot discipline (no game access): the single gate every formation leg
 * shares. Dressing, rallying and returning only happen while the unit has
 * nothing to shoot, nothing ready to shoot with, no fresh wounds and no
 * threat in range - a shot, a cooldown or an incoming round vetoes the
 * slot that frame, by definition. This is what stops the slot/retreat
 * flap: the two can never both claim one frame.
 */
object SlotDiscipline {

  /** Damage older than this stops vetoing formation legs. */
  val damageSilenceFrames: Int = 60

  def maySlot(
    hasAttackTarget: Boolean,
    weaponReady: Boolean,
    framesSinceDamage: Int,
    threatsInRange: Boolean): Boolean =
    ! hasAttackTarget && ! weaponReady &&
      framesSinceDamage >= damageSilenceFrames && ! threatsInRange
}
