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

  /**
   * How far beyond our firing range, in tiles (1 tile = 32 px, as in Atlantis),
   * we still hold the line. Inside this band the unit steps in and shoots
   * instead of posing: at dragoon scale one game step covers it.
   */
  val holdReleaseTiles: Double = 1.1

  /** Fraction of slot distances inside tolerance. Empty means nobody is missing, so fully formed. */
  def fractionInSlot(distancesToSlot: Iterable[Double], tolerancePixels: Double = slotTolerancePixels): Double = {
    val distances = distancesToSlot.toSeq
    if (distances.isEmpty) return 1.0

    distances.count(_ <= tolerancePixels).toDouble / distances.size
  }

  /**
   * Hold the line while the target is well beyond our range, in tiles.
   * Inside the release band (<= 1.1 tiles beyond range) the fight starts:
   * the unit steps in and shoots. The hold also releases when the fight is
   * already on: a cluster mate engaging, or an enemy in range whose weapon
   * is cooling down (it opened fire). Melee must still close (it cannot fight
   * from a slot), flankers must still circle, and without a formation there
   * is nothing to hold.
   * The hold applies only to armed enemy combat units (targetHoldable):
   * buildings - even shooting ones like Sunkens - and unarmed units are
   * always attacked normally instead of being posed against.
   */
  def shouldHoldLine(
    tilesBeyondRange  : Double,
    melee             : Boolean,
    hasFormation      : Boolean,
    flanking          : Boolean,
    allyEngaged       : Boolean,
    enemyFired        : Boolean,
    targetHoldable    : Boolean = true): Boolean = {
    hasFormation && ! melee && ! flanking && ! allyEngaged && ! enemyFired && targetHoldable && tilesBeyondRange > holdReleaseTiles
  }

  /**
   * Advance once this fraction sits in its slots. Below 1.0 on purpose:
   * waiting for every last straggler stalls the march, so a fifth may lag.
   */
  var advanceFormationFraction: Double = 0.8

  /** Step the line forward once enough units sit in their slots. */
  def shouldAdvanceInFormation(fractionUnitsInSlot: Double): Boolean = {
    fractionUnitsInSlot >= advanceFormationFraction
  }
}

