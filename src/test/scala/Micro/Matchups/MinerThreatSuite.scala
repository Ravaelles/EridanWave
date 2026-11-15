package Micro.Matchups

import org.scalatest.funsuite.AnyFunSuite

class MinerThreatSuite extends AnyFunSuite {

  test("only enemy-metro miners gathering in reach threaten") {
    assert(MinerThreat.threatened(metroOurs = false, gathering = true, approachPixels = 0.0, rangeGroundPixels = 100.0))
    assert( ! MinerThreat.threatened(metroOurs = true, gathering = true, approachPixels = 0.0, rangeGroundPixels = 100.0))
    assert( ! MinerThreat.threatened(metroOurs = false, gathering = false, approachPixels = 0.0, rangeGroundPixels = 100.0))
    assert( ! MinerThreat.threatened(metroOurs = false, gathering = true, approachPixels = 296.0, rangeGroundPixels = 100.0))
    assert(MinerThreat.threatened(metroOurs = false, gathering = true, approachPixels = 295.9, rangeGroundPixels = 100.0))
  }
}
