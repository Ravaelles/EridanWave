package Tactic.Squads

/**
 * Pure place-changing decision for Ace pilots (no game access).
 *
 * The caller reads the world (base at the squad vicinity, whether it was
 * scouted recently, whether it shows enemies, stale bases, known enemy
 * bases); this only decides which base name to hold or move to. That split
 * keeps the whole explore contract unit-testable: holding a fight, holding
 * an unseen latch, falling back to the likeliest enemy base on an empty
 * visit, random reevaluation, and running out of bases.
 */
object AceExploreLogic {

  /**
   * @param current           base at the squad vicinity, if any
   * @param currentScouted    the current base was scouted recently (we saw it)
   * @param currentHasEnemies the current base shows enemies right now
   * @param stale             stale base names to explore (may be empty)
   * @param fallback          known enemy bases holding enemies, best first (may be empty)
   * @param roll              random roll; the caller draws it, we index with it
   * @return base name to hold or move to
   */
  def pick(
    current           : Option[String],
    currentScouted    : Boolean,
    currentHasEnemies : Boolean,
    stale             : Seq[String],
    fallback          : Seq[String],
    roll              : Int): Option[String] = {

    if (current.isDefined && (currentHasEnemies || ! currentScouted)) {
      return current
    }

    val random = if (stale.isEmpty) None else Some(stale(Math.floorMod(roll, stale.size)))
    if (current.isDefined) fallback.headOption.orElse(random) else random.orElse(fallback.headOption)
  }
}
