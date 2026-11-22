package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

/**
 * Picked-lurker standoff (Sunkens3 open item): the reaver duel chases a
 * picked Lurker inside static cover and dies there. Holding scarab
 * range instead of closing converts the donation into a standoff the
 * grind can work around. Pure booleans for the suite; the duel leg
 * reads the cover.
 */
class ReaverLurkerHoldSuite extends AnyFunSuite {

  test("a covered lurker is held, everything else walks") {
    assert(ReaverLurkerHold.shouldHold(isLurker = true, staticCoversFoe = true))
    assert( ! ReaverLurkerHold.shouldHold(isLurker = true, staticCoversFoe = false))
    assert( ! ReaverLurkerHold.shouldHold(isLurker = false, staticCoversFoe = true))
    assert( ! ReaverLurkerHold.shouldHold(isLurker = false, staticCoversFoe = false))
  }
}
