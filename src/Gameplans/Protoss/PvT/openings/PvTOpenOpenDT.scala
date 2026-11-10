package Gameplans.Protoss.PvT.openings

import ProxyBwapi.Races.{Protoss, Terran}

/** One PvT opening recipe; mixed into PvTOpeners. Verbatim move. */
trait PvTOpenOpenDT { self: Gameplans.Protoss.PvT.PvTOpeners =>

  def openDT(): Unit = {
    openGateCore()
    scoutOn(Protoss.CyberneticsCore)
    once(15, Protoss.Probe)
    once(2, Protoss.Pylon)
    once(16, Protoss.Probe)
    once(Protoss.Dragoon)
    once(17, Protoss.Probe)
    sneakyCitadel()
    once(19, Protoss.Probe)
    once(2, Protoss.Dragoon)
    once(20, Protoss.Probe)
    once(3, Protoss.Pylon)
    once(21, Protoss.Probe)
    once(Protoss.TemplarArchives)
    once(3, Protoss.Dragoon)
    once(2, Protoss.Nexus)
    once(Protoss.DarkTemplar)
    openingComplete ||= units(Protoss.Nexus) > 1 && units(Protoss.DarkTemplar) > 0
  }
}
