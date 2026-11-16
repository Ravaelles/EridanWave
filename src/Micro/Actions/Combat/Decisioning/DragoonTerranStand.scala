package Micro.Actions.Combat.Decisioning

/**
 * Pure dragoon-vs-Terran stand decision (no game access): a Dragoon with
 * fresh shields and a hot gun shoots the Terran in range instead of
 * running from it. Sixteen shields is a quarter of the blue bar - a
 * chewed hull still runs; six frames is a fifth of the cycle - a cold
 * gun still kites. The caller feeds wound, cooldown and contact; the
 * race gate lives in the caller.
 */
object DragoonTerranStand {

  /** Stand while the shields miss at most this, in hit points. */
  val maxShieldWound: Int = 16

  /** Stand while the weapon needs at most this many frames. */
  val readyCooldownFrames: Int = 6

  def shouldStand(shieldWound: Int, cooldownLeft: Int, terranInRange: Boolean): Boolean =
    shieldWound <= maxShieldWound &&
      cooldownLeft <= readyCooldownFrames &&
      terranInRange
}
