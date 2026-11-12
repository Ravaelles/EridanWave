package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class ThreatPrioritySuite extends AnyFunSuite {

  test("snipe order is Defiler, then Lurker, then Mutalisk, then the rest") {
    assert(ThreatPriority.tier(isDefiler = true, isLurker = false, isMutalisk = false) == ThreatPriority.defilerTier)
    assert(ThreatPriority.tier(isDefiler = false, isLurker = true, isMutalisk = false) == ThreatPriority.lurkerTier)
    assert(ThreatPriority.tier(isDefiler = false, isLurker = false, isMutalisk = true) == ThreatPriority.mutaliskTier)
    assert(ThreatPriority.tier(isDefiler = false, isLurker = false, isMutalisk = false) == ThreatPriority.otherTier)
    assert(ThreatPriority.defilerTier < ThreatPriority.lurkerTier)
    assert(ThreatPriority.lurkerTier < ThreatPriority.mutaliskTier)
    assert(ThreatPriority.mutaliskTier < ThreatPriority.otherTier)
  }

  test("only a visible target of the wanted tier diverts the ladder") {
    assert(ThreatPriority.snipable(visible = true, ThreatPriority.defilerTier, ThreatPriority.defilerTier))
    assert( ! ThreatPriority.snipable(visible = false, ThreatPriority.defilerTier, ThreatPriority.defilerTier))
    assert( ! ThreatPriority.snipable(visible = true, ThreatPriority.lurkerTier, ThreatPriority.defilerTier))
    assert( ! ThreatPriority.snipable(visible = true, ThreatPriority.otherTier, ThreatPriority.mutaliskTier))
  }
}
