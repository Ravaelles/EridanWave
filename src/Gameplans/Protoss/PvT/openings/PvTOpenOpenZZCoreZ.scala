package Gameplans.Protoss.PvT.openings

import ProxyBwapi.Races.{Protoss, Terran}

/** One PvT opening recipe; mixed into PvTOpeners. Verbatim move. */
trait PvTOpenOpenZZCoreZ { self: Gameplans.Protoss.PvT.PvTOpeners =>

  def openZZCoreZ(): Unit = {
    // Reference: https://www.youtube.com/watch?v=MXYRhJOmOkc
    once(8,  Protoss.Probe)
    once(Protoss.Pylon)
    once(10, Protoss.Probe)
    once(Protoss.Gateway)
    scoutOn(Protoss.Gateway)
    once(12, Protoss.Probe)
    once(2, Protoss.Pylon)
    once(13, Protoss.Probe)
    once(Protoss.Zealot)
    doOpeningReactions()
    once(14, Protoss.Probe)
    once(Protoss.Assimilator)
    once(15, Protoss.Probe)
    once(2, Protoss.Zealot)
    once(16, Protoss.Probe)
    once(Protoss.CyberneticsCore)
    once(17, Protoss.Probe)
    once(3, Protoss.Zealot)
    once(18, Protoss.Probe)
    once(3, Protoss.Pylon)
    once(20, Protoss.Probe)
    once(Protoss.DragoonRange)
    once(Protoss.Dragoon)
    once(21, Protoss.Probe)
    once(2, Protoss.Nexus)
    openingComplete ||= units(Protoss.Nexus) > 1 && unitsEver(Protoss.Dragoon) > 0
  }
}
