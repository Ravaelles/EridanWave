package Micro.Targeting

/**
 * Stable-incumbent stickiness (bug-hunt fix): the anchor is the last
 * pick that WON, never the last pick that was FORCED. Why: the old
 * anchor (`toAttackLast`) moved on every jump, including jumps forced
 * by a frame of illegality (kiting out of range, a filter flicker).
 * When the incumbent re-legalized a frame later, the anchor had
 * already followed the jump - so the bonus sat on the new pick and
 * the duel random-walked across whoever was legal each frame (live
 * 4+3: seven targets, flips every ~10 frames, volleys spread over
 * regenerating shields). Now an illegal-but-live anchor survives the
 * jump: the jump is a loan, not a verdict. Releases: the anchor died
 * (permanent - re-anchor the new pick), or it stayed legal and lost
 * fairly (the challenger beat it past KeepCurrent - re-anchor that).
 * No timer needed: death is the release, liveness the hold.
 */
object StickyAnchor {

  def next(anchor: Option[Int], anchorAlive: Boolean, anchorLegal: Boolean, pickId: Int): Option[Int] =
    anchor match {
      case None                     => Some(pickId)
      case Some(a) if a == pickId   => anchor
      case Some(_) if ! anchorAlive => Some(pickId)
      case _ if anchorLegal          => Some(pickId)
      case _                         => anchor
    }
}
