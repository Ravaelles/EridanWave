package Tactic.Squads.Decisions

/**
 * Retreat-forbidden ground (pure numbers, owner order): when the enemy
 * stands on our natural choke - or has already slipped past it toward
 * the main, up to four tiles early - and we stand our ground inside
 * dueling range, the line fights where it stands instead of walking
 * home through the enemy. One predicate, not two: past-the-choke and
 * at-the-choke are the same comparison against the choke radius, so a
 * ghost at the release edge cannot flap the verdict. PvP and PvZ only:
 * Terran sieges outrange the stand and keep their own doctrine. Ground
 * tiles throughout: the choke is a ground concept, never a crow line.
 */
object RetreatForbidden {

  /** The stand opens this far outside the choke radius, in tiles. */
  val chokeMarginTiles: Double = 4.0

  /** This close to that enemy we hold instead of walking home, in tiles. */
  val duelRadiusTiles: Double = 15.0

  /** Release band past the entry edges, in tiles: the latch absorbs mill noise. */
  val releaseMarginTiles: Double = 1.0

  /** Release reach past the duel radius, in tiles. */
  val releaseReachTiles: Double = 2.0

  def forbidden(
    enemyIsProtossOrZerg: Boolean,
    foeMainTiles: Double,
    homeRadiusTiles: Double,
    ourDistToFoeTiles: Double): Boolean =
    enemyIsProtossOrZerg &&
      foeMainTiles <= homeRadiusTiles + chokeMarginTiles &&
      ourDistToFoeTiles <= duelRadiusTiles

  def released(
    foeMainTiles: Double,
    homeRadiusTiles: Double,
    ourDistToFoeTiles: Double): Boolean =
    foeMainTiles > homeRadiusTiles + chokeMarginTiles + releaseMarginTiles ||
      ourDistToFoeTiles > duelRadiusTiles + releaseReachTiles

  /** A pack holds its natural: this many warriors stand instead of running. */
  val minDefenders: Int = 7

  /**
   * Natural defense (pure numbers for the suite): with at least a handful
   * of hulls home and an enemy warrior inside our home radius of the
   * natural, the line fights the foe at the door instead of walking
   * home through it. Any race - masonry defends the same wall.
   */
  def defendNatural(ourWarriors: Int, foeNaturalTiles: Double, homeRadiusTiles: Double): Boolean =
    ourWarriors >= minDefenders && foeNaturalTiles <= homeRadiusTiles

  /** Natural release (pure numbers for the suite): the foe walked out past the margin. */
  def releasedNatural(foeNaturalTiles: Double, homeRadiusTiles: Double): Boolean =
    foeNaturalTiles > homeRadiusTiles + releaseMarginTiles
}
