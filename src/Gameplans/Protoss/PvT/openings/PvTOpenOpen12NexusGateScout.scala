package Gameplans.Protoss.PvT.openings

import Lifecycle.With
import ProxyBwapi.Races.Protoss

/** One PvT opening recipe; mixed into PvTOpeners. Verbatim move. */
trait PvTOpenOpen12NexusGateScout { self: Gameplans.Protoss.PvT.PvTOpeners =>

  def open12NexusGateScout(): Unit = {
    // Reference: https://www.youtube.com/watch?v=AuIqTCxQ1PY
    // Flash says cross-scout; if we verify cross-spawn I believe the correct response is go direct to one gate core, maybe depending on gas.
    With.blackboard.crossScout.set(true)
    once(8, Protoss.Probe)
    once(Protoss.Pylon)
    once(9, Protoss.Probe)
    doOpeningReactions()
    once(12, Protoss.Probe)
    once(2, Protoss.Nexus)
    once(14, Protoss.Probe)
    once(Protoss.Gateway)
    scoutOn(Protoss.Gateway)
    once(15, Protoss.Probe)
    once(2, Protoss.Gateway)
    once(16, Protoss.Probe)
    once(Protoss.Assimilator)
    once(17, Protoss.Probe)
    once(Protoss.CyberneticsCore)
    once(2, Protoss.Zealot)
    once(18, Protoss.Probe)
    once(2, Protoss.Pylon)
    once(19, Protoss.Probe)
    once(2, Protoss.Dragoon)
    once(20, Protoss.Probe)
    get(Protoss.DragoonRange)
    once(21, Protoss.Probe)
    // Flash follows with robo, range, and fast arbiter, and thus takes 2nd gas here
    // TODO: I think one gate core if cross-spawn, two if close. Also might depend on gas/no gas
    // Then: Robo if rax expand; range if not
    openingComplete ||= units(Protoss.Nexus) > 1 && unitsEver(Protoss.Dragoon) > 1
  }
}
