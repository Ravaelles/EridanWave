package Gameplans.Protoss.PvT.openings

import ProxyBwapi.Races.{Protoss, Terran}

/** One PvT opening recipe; mixed into PvTOpeners. Verbatim move. */
trait PvTOpenOpen29Arbiter { self: Gameplans.Protoss.PvT.PvTOpeners =>

  def open29Arbiter(): Unit = {
    // https://tl.net/forum/bw-strategy/538921-pvt-revolution-29-arbiter-build
    gasWorkerFloor(3)
    once(8, Protoss.Probe)
    once(Protoss.Pylon)
    once(9, Protoss.Probe)
    once(Protoss.Assimilator)
    once(11, Protoss.Probe)
    once(Protoss.Gateway)
    once(14, Protoss.Probe)
    once(Protoss.CyberneticsCore)
    once(15, Protoss.Probe)
    once(2, Protoss.Pylon)
    scoutOn(Protoss.Pylon, 2)
    once(18, Protoss.Probe)
    once(Protoss.CitadelOfAdun)
    once(Protoss.Dragoon)
    once(19, Protoss.Probe)
    once(3, Protoss.Pylon)
    once(21, Protoss.Probe)
    once(Protoss.Stargate, Protoss.TemplarArchives)
    once(24, Protoss.Probe)
    requireBases(2)
    once(27, Protoss.Probe)
    once(Protoss.ArbiterTribunal, Protoss.DarkTemplar)
    once(31, Protoss.Probe)
    once(Protoss.Arbiter)
    once(2, Protoss.Dragoon)
    openingComplete ||= unitsEver(Protoss.Arbiter) >= 1 && unitsEver(Protoss.Dragoon) >= 2
  }
}
