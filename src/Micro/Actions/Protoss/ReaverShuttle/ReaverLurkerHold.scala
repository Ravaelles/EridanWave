package Micro.Actions.Protoss.ReaverShuttle

/**
 * Picked-lurker standoff (Sunkens3 open item): the reaver duel chases a
 * picked Lurker inside static cover and dies there. Holding scarab
 * range instead of closing converts the donation into a standoff the
 * grind can work around. Pure booleans for the suite; the duel leg
 * reads the cover.
 */
object ReaverLurkerHold {
  /** Hold instead of closing: the picked lurker sits under static guns. */
  def shouldHold(isLurker: Boolean, staticCoversFoe: Boolean): Boolean =
    isLurker && staticCoversFoe
}
