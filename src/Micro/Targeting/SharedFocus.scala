package Micro.Targeting

/**
 * Latched shared pick (the focus-memory fix): the pack's committed
 * target survives reach churn. `FocusFire.choose` and `MeleeFocus.choose`
 * re-derive the optimum from instantaneous geometry every frame with
 * zero hysteresis - in a static ball the inputs sit still and focus
 * works, in a moving ball the max-reach crown moves every few frames
 * and the pack re-votes instead of finishing (live 4+3: four dragoons
 * on seven targets, flips every ~10 frames, volleys spread over
 * regenerating shields, 1/7). The ladder's KeepCurrent stickiness
 * never sees these frames: the shared pick returns before the ladder.
 *
 * Hold rules, pinned beside the seams that read them: the incumbent
 * stays while it is still a candidate with at least one shooter in
 * reach, even as the crown moves around it. Releases: target gone
 * (dead, invisible, illegal), nobody left in reach, or a challenger
 * that dies to one volley while the incumbent does not (take the
 * free kill, never grind past it). A merely cheaper grind does NOT
 * preempt - finishing owns the frame. No incumbent means no hold:
 * entry keeps its quorum gates untouched.
 */
object SharedFocus {

  /** One pack-level bid: target id, shooters in reach, volleys to finish. */
  final case class Bid(id: Int, reach: Int, costToKill: Int)

  def hold(current: Option[Bid], bids: Seq[Bid]): Option[Bid] =
    current.flatMap(incumbent =>
      bids.find(_.id == incumbent.id).filter(_.reach >= 1).flatMap(alive =>
        bids.filter(b => b.id != alive.id && b.costToKill == 1 && alive.costToKill > 1)
          .sortBy(_.id).headOption.orElse(Some(alive))))
}
