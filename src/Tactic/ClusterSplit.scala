package Tactic

/**
 * Pure main-cluster split (no game access): most of the army always stays
 * with the main cluster, or contains collapse the moment home needs bodies.
 * At most forty-five percent of warriors staff everything else (base
 * defense, expo raids); detachments against real attacks never go below
 * five warriors, so nothing trickles in to die free. Lone-harasser guards
 * are exempt: one body answers one pest.
 */
object ClusterSplit {

  /** Share of warriors that must stay with the main cluster. */
  val mainShare: Double = 0.55

  /** Smallest detachment worth sending at a real attack, in warriors. */
  val minDetachment: Int = 5

  /**
   * Warriors allowed outside the main cluster, rounded down. Integer math
   * on purpose: 1.0 - 0.55 is 0.44999 in doubles and would steal one body
   * per hundred from the delegation.
   */
  def maxDelegated(totalWarriors: Int): Int =
    totalWarriors - (totalWarriors * 55) / 100

  def overDelegated(totalWarriors: Int, delegated: Int): Boolean =
    delegated > maxDelegated(totalWarriors)

  def detachmentFloor(sizedNeed: Int): Int =
    Math.max(minDetachment, sizedNeed)
}
