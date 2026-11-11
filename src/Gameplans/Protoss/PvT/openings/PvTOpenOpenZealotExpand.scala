package Gameplans.Protoss.PvT.openings

import ProxyBwapi.Races.Protoss

/** One PvT opening recipe; mixed into PvTOpeners. Verbatim move. */
trait PvTOpenOpenZealotExpand { self: Gameplans.Protoss.PvT.PvTOpeners =>

  def openZealotExpand(): Unit = {
    // References:
    // #1 https://youtu.be/jYRHZVAjhX8?t=4932
    // #2 https://www.youtube.com/watch?v=9uKU2hevkCQ
    // #3 https://discord.com/channels/828899288616140811/890891217636818964/915594910122516520
    // This build is the one in #2/#3. #1 delays Pylon and lets Nexus produce the supply, which gets you faster Dragoon but worse economy.
    once(8, Protoss.Probe)
    once(Protoss.Pylon)
    once(10, Protoss.Probe)
    once(Protoss.Gateway)
    once(13, Protoss.Probe)
    once(Protoss.Zealot)
    doOpeningReactions()
    once(14, Protoss.Probe)
    once(2, Protoss.Nexus)
    scoutOn(Protoss.Nexus, quantity = 2)
    once(Protoss.Assimilator)
    once(2, Protoss.Pylon)
    once(15, Protoss.Probe)
    once(Protoss.CyberneticsCore)
    once(16, Protoss.Probe)
    once(2, Protoss.Zealot)
    once(17, Protoss.Probe)
    once(Protoss.Dragoon)
    openingComplete ||= units(Protoss.Nexus) > 1 && units(Protoss.Dragoon) > 1
  }
}
