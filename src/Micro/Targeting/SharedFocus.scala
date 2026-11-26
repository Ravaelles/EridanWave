package Micro.Targeting

/**
 * Latched shared shortlist (the pack's kill queue, owner order): the
 * pack designates its targets once, in kill order, and every hull
 * without a pick of its own takes the first one it can hit - trying
 * from the first to the last instead of dueling alone. A single pick
 * cannot work: reach is per-hull and never unanimous, so a lone
 * shared target strands every hull outside of it. The queue fixes
 * exactly that while keeping one decision for the whole pack.
 *
 * Order rules, pinned beside the seams that read them: committed
 * picks keep their order no matter how reach churns (no re-sort, no
 * re-vote - the flip source on live mirrors); dead picks drop out;
 * a one-volley free kill jumps the queue (take it, never grind past
 * it); newcomers append behind in choose order (widest reach, then
 * fewest volleys, then id); the list caps at packTargets, worst first
 * out. A merely cheaper grind does NOT jump the queue - finishing
 * owns the frame.
 */
object SharedFocus {

  /** Pack shortlist length: how many pack-designated targets a hull scans, first to last. */
  val packTargets: Int = 3

  /** One pack-level bid: target id, shooters in reach, volleys to finish. */
  final case class Bid(id: Int, reach: Int, costToKill: Int)

  private def newcomersFirst(bids: Seq[Bid]): Seq[Bid] =
    bids.sortBy(b => (-b.reach, b.costToKill, b.id))

  /**
   * Pack kill queue (pure numbers for the suite): stable committed
   * order, dead picks dropped, free kills jumped to the front,
   * newcomers appended in choose order, capped at max (worst first
   * out). No incumbent means a fresh queue in choose order.
   */
  def orderPack(current: Vector[Int], bids: Seq[Bid], max: Int): Vector[Int] = {
    val alive = bids.map(_.id).toSet
    val survivors = current.filter(alive.contains)
    val free = newcomersFirst(bids.filter(b => b.costToKill == 1 && ! survivors.contains(b.id))).map(_.id)
    val fresh = newcomersFirst(bids.filter(b => ! survivors.contains(b.id) && b.costToKill > 1)).map(_.id)
    (free ++ survivors ++ fresh).distinct.take(math.max(1, max)).toVector
  }
}
