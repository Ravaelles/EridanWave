package Micro.Actions.Protoss.Reaver

import org.scalatest.funsuite.AnyFunSuite

class ReaverThreatRangesSuite extends AnyFunSuite {

  test("the engage radius is in tiles") {
    assert(ReaverThreatRanges.inReach(8.5))
    assert(ReaverThreatRanges.inReach(0.0))
    assert( ! ReaverThreatRanges.inReach(8.51))
  }
}
