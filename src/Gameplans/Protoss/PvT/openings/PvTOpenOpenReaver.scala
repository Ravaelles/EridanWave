package Gameplans.Protoss.PvT.openings

import ProxyBwapi.Races.Protoss

/** One PvT opening recipe; mixed into PvTOpeners. Verbatim move. */
trait PvTOpenOpenReaver { self: Gameplans.Protoss.PvT.PvTOpeners =>

  def openReaver(): Unit = {
    openGateCore()
    once(14, Protoss.Probe)
    once(Protoss.Zealot)
    once(2, Protoss.Pylon)
    once(16, Protoss.Probe)
    scoutOn(Protoss.Probe, quantity = 16)
    once(Protoss.Dragoon)
    once(17, Protoss.Probe)
    once(3, Protoss.Pylon)
    once(Protoss.RoboticsFacility)
    once(18, Protoss.Probe)
    once(2, Protoss.Dragoon)
    once(19, Protoss.Probe)
    once(3, Protoss.Dragoon)
    // Just spitballing this part; not refined
    once(Protoss.Shuttle)
    once(4, Protoss.Pylon)
    once(20, Protoss.Probe)
    once(Protoss.RoboticsSupportBay)
    once(21, Protoss.Probe)
    once(2, Protoss.Zealot)
    once(Protoss.Reaver)
    once(2, Protoss.Nexus)
    once(5, Protoss.Pylon)
    openingComplete ||= units(Protoss.Nexus) > 1 && units(Protoss.Reaver) > 0
  }
}
