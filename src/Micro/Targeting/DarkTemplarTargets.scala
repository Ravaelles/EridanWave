package Micro.Targeting

/**
 * Pure Dark Templar target priority (no game access): a Templar with no
 * cooldown kills what it can reach, weakest first - wounded Marauders die
 * to one swipe instead of trading with the healthy pack. Then tanks, then
 * warriors, then workers, then anything else hittable. Nearest inside
 * each tier; only real units inside closeTiles count, fogged contacts
 * keep the old nearest-hittable hunt. Distances in tiles.
 */
object DarkTemplarTargets {

  /** Weak enough to die to one swipe, in total hit points. */
  val weakHp: Int = 39

  /** Only real units this close, in tiles, enter the tiered pick. */
  val closeTiles: Double = 0.9

  final case class Mark(distTiles: Double, totalHealth: Int, isTank: Boolean, isWarrior: Boolean, isWorker: Boolean)

  def tier(mark: Mark): Int =
    if (mark.totalHealth <= weakHp) 0
    else if (mark.isTank) 1
    else if (mark.isWarrior) 2
    else if (mark.isWorker) 3
    else 4

  /** Nearest inside the best non-empty tier; nothing when nobody qualifies. */
  def pick(marks: Seq[Mark]): Option[Int] =
    if (marks.isEmpty) None
    else Some(marks.indices.minBy(i => (tier(marks(i)), marks(i).distTiles)))
}
