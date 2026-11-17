package Micro.Actions.Combat.Maneuvering

/**
 * Dodge discipline (no game access): close to our own main, a nearly
 * ready gun holds instead of shuffling - the volley lands before any
 * push matters, and dodging it only trades damage for footwork.
 * Past the natural-choke radius, or while the gun is still cooling,
 * dodging proceeds as before.
 */
object DOdge {

  /** Gun this cold or colder still dodges at home, in frames. */
  val homeCooldownFrames: Int = 6

  /**
   * Dodge allowed (pure flags for the suite): outside the home radius
   * always, inside it only while the gun is still cold.
   */
  def shouldDodge(distMainTiles: Double, homeRadiusTiles: Double, cooldownLeft: Int): Boolean =
    distMainTiles >= homeRadiusTiles || cooldownLeft >= homeCooldownFrames
}
