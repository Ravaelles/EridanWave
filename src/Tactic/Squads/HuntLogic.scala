package Tactic.Squads

import Mathematics.Points.Pixel

/**
 * Pure refresh decision for contact-loss hunting (no game access).
 *
 * The caller reads the world (fogged contacts, unseen tiles, distances,
 * frames); this only decides whether the latched destination stands.
 * That split keeps the whole latch contract unit-testable: hold, reach,
 * expiry, superseding contact, blind search, and running out of map.
 */
object HuntLogic {

  /**
   * @param current    latched destination, if any
   * @param reached    our squad already covers it
   * @param expired    the latch is older than the search window
   * @param contact    freshest fogged contact as (pixel, lastSeen frame), if any
   * @param setFrame   frame the current latch was set on
   * @param unseen     unseen tiles, nearest first (may be empty)
   * @param now        current frame, stamped onto a new latch
   * @return (destination to hold or move to, frame the latch was set on)
   */
  def refresh(
    current   : Option[Pixel],
    reached   : Boolean,
    expired   : Boolean,
    contact   : Option[(Pixel, Int)],
    setFrame  : Int,
    unseen    : Seq[Pixel],
    now       : Int): (Option[Pixel], Int) = {

    val superseded  = contact.exists(_._2 > setFrame)
    val stale       = current.isEmpty || reached || expired || superseded
    if ( ! stale) return (current, setFrame)

    (contact.map(_._1).orElse(unseen.headOption), now)
  }
}
