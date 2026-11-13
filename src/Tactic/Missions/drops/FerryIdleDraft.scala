package Tactic.Missions.drops

/**
 * Pure idle-draft gate (no game access): a healthy Shuttle sitting
 * without a task - or without even a move order - belongs in a ferry
 * instead of on parking duty. Health first: a scratched hull still
 * flies the trip, a bleeding one stays home.
 */
object FerryIdleDraft {

  /** Draft shuttles at or above this fraction of full health. */
  val minHealthFraction: Double = 0.6

  def shouldDraft(healthFraction: Double, hasTask: Boolean, hasMoveOrder: Boolean): Boolean =
    healthFraction >= minHealthFraction && ( ! hasTask || ! hasMoveOrder)
}
