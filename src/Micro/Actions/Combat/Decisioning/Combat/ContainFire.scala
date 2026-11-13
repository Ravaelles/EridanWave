package Micro.Actions.Combat.Decisioning

/**
 * Pure contain-fire decision (no game access): while containing, a ready
 * weapon shoots the nearest hittable target instead of dressing, kiting
 * or repositioning past it. Volley discipline and holds own every other
 * fight - this override exists only for the contain, where the line must
 * not dance around free kills.
 */
object ContainFire {

  def shouldShoot(containing: Boolean, cooldownLeft: Int, hasHittableTarget: Boolean): Boolean =
    containing && hasHittableTarget && cooldownLeft <= 0
}
