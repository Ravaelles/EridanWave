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

  /** Inside the hunt, in ground tiles from our main. */
  def inHuntRange(distToMainTiles: Double): Boolean =
    distToMainTiles <= huntTiles

  /** Nearest to the main; nothing when nobody qualifies. */
  def pick(marks: Seq[Mark]): Option[Int] =
    if (marks.isEmpty) None
    else Some(marks.indices.minBy(i => marks(i).distToMainTiles))
}
