package Macro.Actions

/**
 * Second-base veto (no game access): one home for "would we cancel a
 * morphing Nexus right after ordering it". Ordering and cancelling
 * read the same predicate, so a base the veto forbids never gets
 * ordered in the first place - the order/cancel saw that donated
 * 100 minerals per cycle is structurally impossible instead of
 * merely unlikely.
 *
 * Two different clocks release the veto. Before ten minutes only a
 * commanding position expands at all: below fifteen Dragoons of value
 * the natural never pays for itself (same bar as the reaver backbone,
 * different question), and a stronger enemy means the base feeds the
 * push instead of the bank. Past ten minutes with that backbone the
 * game is decided elsewhere, so the veto lifts and the line doubles.
 * Past twelve the base is unconditional: comebacks need gates, and
 * gates need gas.
 *
 * The cancel side reads the Dark Templar clause only, never the army
 * score: skim scores breathe around parity every few frames, and a
 * veto that flaps with them would eat the nexus-first opener on noise.
 * An unanswered blade with no Forge money is not noise - it kills the
 * morphing base for free, so the refund funds detection instead.
 */
object ExpandForbidden {

  /** Past this game time with the backbone below, expand freely. */
  val allowMinutes: Double = 10.0

  /** Past this army value with ten minutes, expand freely. */
  val allowArmyUnits: Double = 160.0

  /** Past this game time the second base is unconditional. */
  val forceMinutes: Double = 12.0

  /**
   * Ordering veto (pure numbers for the suite): forbid while weak,
   * behind or Dark-Templar-threatened, unless the game clock or the
   * backbone already earned the base.
   */
  def check(
    minutes: Double,
    armyStrengthUnits: Double,
    enemyStronger: Boolean,
    darkTemplarShown: Boolean,
    detectionReady: Boolean,
    forgeAffordable: Boolean): Boolean = {
    if (minutes >= forceMinutes) false
    else if (minutes >= allowMinutes && armyStrengthUnits >= allowArmyUnits) false
    else enemyStronger || (darkTemplarShown && ! detectionReady && ! forgeAffordable)
  }

  /**
   * Cancel veto (pure numbers for the suite): an in-flight morphing
   * Nexus dies only to an unanswered blade with no Forge money - the
   * imminent-doom case the refund rescues. Never the army score: it
   * breathes, and breathing must not eat bases.
   */
  def shouldCancelBase(
    minutes: Double,
    morphing: Boolean,
    darkTemplarShown: Boolean,
    detectionReady: Boolean,
    forgeAffordable: Boolean): Boolean =
    morphing &&
      minutes < forceMinutes &&
      darkTemplarShown &&
      ! detectionReady &&
      ! forgeAffordable
}
