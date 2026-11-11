package Gameplans.Protoss.PvT.openings

import ProxyBwapi.Races.Protoss

/** One PvT opening recipe; mixed into PvTOpeners. Verbatim move. */
trait PvTOpenOpen28Nexus { self: Gameplans.Protoss.PvT.PvTOpeners =>

  def open28Nexus(): Unit = {
    // Reference: https://liquipedia.net/starcraft/28_Nexus_(vs._Terran)
    openGateCore()
    scoutOn(Protoss.CyberneticsCore) // I think if BBS is possibility you might want to cross-spawn
    once(15, Protoss.Probe)
    once(2, Protoss.Pylon)
    once(16, Protoss.Probe)
    once(Protoss.Dragoon)
    once(17, Protoss.Probe)
    once(Protoss.DragoonRange)
    if (gasCapsUntouched && unitsEver(Protoss.Dragoon) > 0 && haveGasForUpgrade(Protoss.DragoonRange)) {
      gasWorkerCeiling(2)
    }
    once(19, Protoss.Probe)
    once(2, Protoss.Dragoon)
    once(20, Protoss.Probe)
    once(3, Protoss.Pylon)
    once(21, Protoss.Probe)
    once(3, Protoss.Dragoon)
    once(2, Protoss.Nexus)
    once(22, Protoss.Probe)
    once(4, Protoss.Dragoon)
    openingComplete ||= unitsEver(Protoss.Dragoon) > 3
  }
}
