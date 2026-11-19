package Micro.Targeting

/**
 * Main-base Dark Templar hunt (no game access): a pre-`Target` doctrine
 * pick (conventions rule 15), one target with a fixed priority, acting
 * before retreat and before `Target.choose`. A detected enemy Templar
 * inside forty ground tiles of our main must die - it kills workers
 * for free while the ladder scores shinier fights elsewhere. Nearest
 * to the main wins; the main never moves, so the pick cannot flap
 * between two hunters the way nearest-to-me does. Distances in tiles.
 */
object DarkTemplarHunt {

  /** Hunt detected Templars this close to our main, in ground tiles. */
  val huntTiles: Double = 40.0

  final case class Mark(distToMainTiles: Double)

  /**
   * Small-army reach, in ground tiles: below thirty warriors the whole
   * army answers a blade at the workers, so the hunt walks ten tiles
   * further out instead of holding the old line while miners die. Large
   * armies keep the old forty - they must not all march home for one
   * blade while the front needs them.
   */
  val smallArmyWarriors: Int = 30
  val farHuntTiles: Double = 50.0

  def huntRangeTiles(warriors: Int): Double =
    if (warriors < smallArmyWarriors) farHuntTiles else huntTiles

  /** Inside the hunt, in ground tiles from our main, at this army size. */
  def inHuntRange(distToMainTiles: Double, warriors: Int): Boolean =
    distToMainTiles <= huntRangeTiles(warriors)

  /** Nearest to the main; nothing when nobody qualifies. */
  def pick(marks: Seq[Mark]): Option[Int] =
    if (marks.isEmpty) None
    else Some(marks.indices.minBy(i => marks(i).distToMainTiles))
}
