package Micro.Actions.Protoss.Dragoon

/**
 * Shielded Dragoon carrier hunt (no game access): a Dragoon holding twenty
 * shields always answers a Carrier in range, and walks a pathed Carrier
 * inside eleven tiles - to the hull with a ground path, else to the
 * halfway point with one. Interceptors chew unshielded hulls for free,
 * so the rule only speaks for shields that can pay for the trade.
 */
object DragoonCarrierHunt {

  /** Shields that can pay for the Carrier trade. */
  val shieldFloor: Int = 20

  /** Carriers past this never get chased, in tiles. */
  val huntTiles: Double = 11.0

  /** Shoot: a shielded Dragoon always answers a Carrier in range. */
  def shouldShoot(isDragoon: Boolean, shields: Int, carrierInRange: Boolean): Boolean =
    isDragoon && shields >= shieldFloor && carrierInRange

  /**
   * Close: a shielded Dragoon walks a pathed Carrier inside eleven tiles.
   * Pure numbers for the suite; the caller checks the full path first
   * and the halfway path second.
   */
  def shouldClose(isDragoon: Boolean, shields: Int, carrierTiles: Double, pathed: Boolean): Boolean =
    isDragoon && shields >= shieldFloor && carrierTiles <= huntTiles && pathed
}
