package Gameplans.Protoss.PvT.openings

import ProxyBwapi.Races.{Protoss, Terran}

/** One PvT opening recipe; mixed into PvTOpeners. Verbatim move. */
trait PvTOpenOpen20Rangeless { self: Gameplans.Protoss.PvT.PvTOpeners =>

  def open20Rangeless(): Unit = {
    // Reference: https://www.youtube.com/watch?v=gCtO31tlrBs
    openGateCore()
    once(15, Protoss.Probe)
    once(2, Protoss.Pylon)
    once(16, Protoss.Probe)
    once(Protoss.Dragoon)
    once(18, Protoss.Probe)
    once(2, Protoss.Nexus)
    scoutOn(Protoss.Nexus, quantity = 2)
    once(2, Protoss.Dragoon)
    once(19, Protoss.Probe)
    if (gasCapsUntouched) {
      gasWorkerCeiling(Math.max(1, unitsComplete(Protoss.Probe) - 12))
    }
    openingComplete ||= units(Protoss.Nexus) > 1 && unitsEver(Protoss.Dragoon) > 1
  }
}
