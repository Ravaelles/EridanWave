package Tactic.Squads.Decisions

/**
 * Retreat-forbidden ground (pure numbers, owner order): when the enemy
 * stands on our natural choke or has already slipped past it toward
 * the main, and we stand our ground within dueling range, retreating
 * donates the choke and the game with it - the line fights where it
 * stands instead of walking home through the enemy. PvP and PvZ only:
 * Terran sieges outrange the stand and keep their own doctrine. Ground
 * tiles throughout: the choke is a ground concept, never a crow line.
 */
object RetreatForbidden {

  /** An enemy this close to the natural choke owns it, in tiles. */
  val chokeRadiusTiles: Double = 4.0

  /** This close to that enemy we hold instead of walking home, in tiles. */
  val duelRadiusTiles: Double = 15.0

  def forbidden(
    enemyIsProtossOrZerg: Boolean,
    foeChokeTiles: Double,
    foePastChoke: Boolean,
    ourDistToFoeTiles: Double): Boolean =
    enemyIsProtossOrZerg &&
      (foeChokeTiles <= chokeRadiusTiles || foePastChoke) &&
      ourDistToFoeTiles <= duelRadiusTiles
}
