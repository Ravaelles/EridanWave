package Micro.Targeting

/**
 * Pure Overlord snipe rule (no game access): Overlords die only when there
 * is nothing better to shoot. Chasing a fleeing Overlord across the map
 * while its friends chew the army is the classic way to split a winning
 * fight in half, so an Overlord is a legal target only inside our firing
 * range with no other hittable enemy nearby. Lone Overlords still die.
 */
object OverlordSnipe {

  def allowed(overlordInRange: Boolean, alternativesInRange: Int): Boolean =
    overlordInRange && alternativesInRange == 0
}
