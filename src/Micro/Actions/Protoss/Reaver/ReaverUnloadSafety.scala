package Micro.Actions.Protoss.Reaver

/**
 * Pure unload-spot sizing (no game access): a wounded Reaver needs more
 * room around the drop (Atlantis IsNotSafeToUnloadReaverHere).
 */
object ReaverUnloadSafety {

  /**
   * @param woundFraction 0-1 wound fraction (ours, not Atlantis 0-100)
   * @return tiles around the drop that must hold no enemy
   */
  def radiusTiles(woundFraction: Double): Double =
    if (woundFraction > 0.6) 5.7 else if (woundFraction > 0.33) 3.9 else 2.0
}
