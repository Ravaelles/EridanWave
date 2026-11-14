package Micro.Actions.Combat.Decisioning

/**
 * Pure contain-fire decision (no game access): while containing, a ready
 * weapon shoots the nearest hittable target instead of dressing, kiting
 * or repositioning past it. Volley discipline and holds own every other
 * fight - this override exists only for the contain, where the line must
 * not dance around free kills. Containing is the caller's precondition.
 */
object ContainFire {

  def shouldShoot(cooldownLeft: Int, hasHittableTarget: Boolean): Boolean =
    hasHittableTarget && cooldownLeft <= 0
}
