package Tactic.Squads.Decisions

/**
 * Slot discipline (no game access): the single gate every formation leg
 * shares. Dressing, rallying and returning only yield to an actual shot -
 * a hot gun with a hittable target outranks the pose - to fresh wounds,
 * and to a held retreat verdict that already owns the frame. Mere
 * contact no longer vetoes: a target on the ladder, a cooling weapon or
 * a distant threat never stopped PurpleWave from dressing, and the line
 * only exists if it forms while the enemy is still out there.
 */
object SlotDiscipline {

  /** Damage older than this stops vetoing formation legs. */
  val damageSilenceFrames: Int = 60

  def maySlot(
    firingNow: Boolean,
    framesSinceDamage: Int,
    verdictHeld: Boolean): Boolean =
    ! firingNow &&
      framesSinceDamage >= damageSilenceFrames && ! verdictHeld
}
