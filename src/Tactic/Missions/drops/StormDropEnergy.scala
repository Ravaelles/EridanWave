package Tactic.Missions.drops

/**
 * Pure storm-energy rule (no game access): a Templar below storm cost is
 * spent. Shared by the manifest matcher and the go-home check so the two
 * cannot drift apart.
 */
object StormDropEnergy {

  /** Energy a storm costs; below this a Templar is spent. */
  val stormEnergy: Int = 75

  /** All Templars spent: time to go home. Empty crew counts as spent. */
  def drained(energies: Seq[Int]): Boolean = energies.forall(_ < stormEnergy)
}
