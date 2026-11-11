package Gameplans.Protoss.PvT.openings

import Lifecycle.With
import ProxyBwapi.Races.Protoss
import Utilities.?

/** One PvT opening recipe; mixed into PvTOpeners. Verbatim move. */
trait PvTOpenOpen1015 { self: Gameplans.Protoss.PvT.PvTOpeners =>

  def open1015(): Unit = {
    // No scout -- let the Dragoons do the work
    openGateCore()
    once(15, Protoss.Probe)
    once(2, Protoss.Gateway)
    // Liquipedia offers three variations; Namu offers a fourth. We prioritize range to hit bunkers ASAP
    once(Protoss.DragoonRange)
    once(1, Protoss.Dragoon)
    once(2, Protoss.Pylon)
    once(3, Protoss.Dragoon)
    once(3, Protoss.Pylon)
    once(5, Protoss.Dragoon)
    val targetNexi = ?(enemyStrategy(With.fingerprints.twoFac, With.fingerprints.threeFac), 2, 3)
    once(targetNexi, Protoss.Nexus)
    openingComplete ||= units(Protoss.Nexus) >= targetNexi
  }
}
