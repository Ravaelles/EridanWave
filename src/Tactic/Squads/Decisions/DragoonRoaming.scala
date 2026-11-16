package Tactic.Squads.Decisions

/**
 * Early Dragoon roaming permission (no game access): our Dragoons roam
 * freely until the enemy shows its first Dragoon. Before that a PvP
 * Dragoon has no ranged answer to dress against - holding slots and
 * rallying only bleeds the map while the enemy takes it for free.
 * The permission closes permanently on first recognition: from then on
 * the line dresses, rallies and returns like every other army.
 */
object DragoonRoaming {

  /**
   * Roam while we are a Dragoon in a mirror with no enemy Dragoon seen.
   * Pure booleans for the suite; the caller reads the scout memory.
   */
  def mayRoam(isDragoon: Boolean, enemyIsProtoss: Boolean, enemyDragoonSeen: Boolean): Boolean =
    isDragoon && enemyIsProtoss && ! enemyDragoonSeen
}
