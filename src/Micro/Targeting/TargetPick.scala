package Micro.Targeting

/**
 * Target pick (pure rows, no game access): the ladder's atom as data.
 * Given scored, legality-flagged candidates plus which one is the
 * current pick, selects the max legal score with KeepCurrent stickiness
 * for the incumbent, or the first legal row when the stratum does not
 * rank. Iteration order breaks ties, exactly like the ladder loop did -
 * reordering rows reorders the verdict, so callers pass sources in
 * ladder order (assigned before matchups). One owner for "who wins",
 * so ladder-shape tests run on recorded rows instead of a live game.
 */
object TargetPick {

  final case class Row(id: Int, score: Double, isCurrent: Boolean, legal: Boolean)

  def pick(rows: Iterable[Row], rankByScore: Boolean): Option[Int] = {
    var bestId        : Int     = -1
    var bestScore     : Double  = 0.0
    var found         : Boolean = false
    var currentId     : Int     = -1
    var currentScore  : Double  = 0.0
    var hasCurrent    : Boolean = false
    val it = rows.iterator
    while (it.hasNext) {
      val row = it.next()
      if (row.legal) {
        if ( ! rankByScore) return Some(row.id)
        if (row.isCurrent) {
          hasCurrent = true
          currentId = row.id
          currentScore = row.score
        } else if ( ! found || row.score > bestScore) {
          found = true
          bestId = row.id
          bestScore = row.score
        }
      }
    }
    if (hasCurrent && ( ! found || KeepCurrent.keepsTarget(currentScore, bestScore))) Some(currentId)
    else if (found) Some(bestId)
    else None
  }
}
