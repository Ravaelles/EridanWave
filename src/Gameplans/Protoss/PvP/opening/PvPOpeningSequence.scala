package Gameplans.Protoss.PvP

import Lifecycle.With
import Mathematics.Maff
import Strategery.Strategies.Protoss._

/** Chooses the core sequence: Gate-first, Tech-first, Gate-heavy or wide 2-Gate. */
trait PvPOpeningSequence { this: PvPOpening =>

  def chooseCoreSequence(): Unit = {
    if (sequence == null) {
      sequence = if (PvP1012()) GGC else {
        var wTech   =   1.0
        var wGate   =   1.0
        wTech       *=  (With.geography.startLocations.length - 1)
        wTech       *=  Maff.or1(0,     With.strategy.isFlat || With.strategy.isInverted)
        wTech       *=  Maff.or1(0,     PvP3GateGoon() || PvP4GateGoon() ||  PvPGateCore())
        wGate       *=  Maff.or1(2,     enemyRecentStrategy(With.fingerprints.proxyGateway, With.fingerprints.twoGate99))
        wGate       *=  Maff.or1(1000,  enemyRecentStrategy(With.fingerprints.twoGate))
        wGate       *=  Maff.or1(.0001, enemyRecentStrategy(With.fingerprints.oneGateCore))
        Maff.sampleWeightedValues(
          (GCRange, 1.0),
          (GCTech,  wTech),
          (GCGate,  wGate)).getOrElse(GCRange)
      }
    }
  }
}
