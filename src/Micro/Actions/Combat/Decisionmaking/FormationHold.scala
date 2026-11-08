package Micro.Actions.Combat.Decisionmaking

/**
 * When to hold the engagement formation instead of breaking it to chase.
 *
 * Stardust forms its arc before contact and only switches to per-unit micro
 * once something is actually in range. Our equivalent: while the enemy is
 * still beyond our firing range, ranged units keep their formation slots and
 * let the enemy come to us. The line steps forward only once every unit sits
 * in its slot.
 *
 * Pure decisions on numbers so they stay unit-testable without a game;
 * Combat supplies the game-aware seam (distances, slots, flags).
 */
object FormationHold {

  /** A unit counts as "in formation" inside one tile of its slot. */
  val slotTolerancePixels: Double = 32.0

  /** Fraction of slot distances inside tolerance. Empty means nobody is missing, so fully formed. */
  def fractionInSlot(distancesToSlot: Iterable[Double], tolerancePixels: Double = slotTolerancePixels): Double = {
    val distances = distancesToSlot.toSeq
    if (distances.isEmpty) return 1.0
    distances.count(_ <= tolerancePixels).toDouble / distances.size
  }

  /**
   * Hold the line while the target is still beyond our range.
   * Melee must still close (it cannot fight from a slot), flankers must
   * still circle, and without a formation there is nothing to hold.
   */
  def shouldHoldLine(pixelsBeyondRange: Double, melee: Boolean, hasFormation: Boolean, flanking: Boolean): Boolean = {
    hasFormation && ! melee && ! flanking && pixelsBeyondRange > 0
  }

  /** Step the line forward only once every unit sits in its slot. */
  def shouldAdvanceInFormation(fractionUnitsInSlot: Double): Boolean = {
    fractionUnitsInSlot >= 1.0
  }
}
