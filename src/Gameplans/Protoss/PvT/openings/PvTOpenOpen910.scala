package Gameplans.Protoss.PvT.openings

import ProxyBwapi.Races.{Protoss, Terran}

/** One PvT opening recipe; mixed into PvTOpeners. Verbatim move. */
trait PvTOpenOpen910 { self: Gameplans.Protoss.PvT.PvTOpeners =>

  def open910(): Unit = {
    once(9, Protoss.Probe)
    once(Protoss.Gateway)
    once(10, Protoss.Probe)
    once(2, Protoss.Gateway)
    once(11, Protoss.Probe)
    once(Protoss.Zealot)
    once(2, Protoss.Pylon)
    once(2, Protoss.Zealot)
    once(12, Protoss.Probe)
    once(3, Protoss.Zealot)
    once(13, Protoss.Probe)
    once(4, Protoss.Zealot)
    doOpeningReactions()
    once(14, Protoss.Probe)
    once(3, Protoss.Pylon)
    once(15, Protoss.Probe)
    once(5, Protoss.Zealot)
    once(16, Protoss.Probe)
    once(Protoss.Assimilator)
    once(17, Protoss.Probe)
    once(Protoss.CyberneticsCore)
    once(19, Protoss.Probe)
    openingComplete ||= unitsEver(Protoss.Dragoon) > 1
  }
}
