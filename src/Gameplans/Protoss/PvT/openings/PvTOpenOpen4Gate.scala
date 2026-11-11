package Gameplans.Protoss.PvT.openings

import Lifecycle.With
import Placement.Access.PlaceLabels.Proxy4Gate
import Placement.Access.{PlaceLabels, PlacementQuery}
import ProxyBwapi.Races.Protoss

/** One PvT opening recipe; mixed into PvTOpeners. Verbatim move. */
trait PvTOpenOpen4Gate { self: Gameplans.Protoss.PvT.PvTOpeners =>

  def open4Gate(): Unit = {
    if (unitsEver(Protoss.Gateway) < 4) {
      gasWorkerCeiling(2)
    }
    once(8, Protoss.Probe)
    get(1, Protoss.Pylon, new PlacementQuery(Protoss.Pylon)
      .preferLabelYes(Proxy4Gate)
      .requireZone(With.geography.zones.filter(_.heart.groundTiles(With.geography.home) > 40): _*)
      .preferBase(With.geography.mains.filterNot(_.isOurMain): _*)
      .preferZone(With.geography.mains.filterNot(_.isOurMain).map(_.zone): _*))
    once(10, Protoss.Probe)
    get(1, Protoss.Gateway, new PlacementQuery(Protoss.Gateway).preferLabelYes(Proxy4Gate))
    scoutOn(Protoss.Gateway)
    once(12, Protoss.Probe)
    once(Protoss.Assimilator)
    once(13, Protoss.Probe)
    doOpeningReactions()
    once(Protoss.CyberneticsCore)
    once(15, Protoss.Probe)
    once(2, Protoss.Pylon)
    once(16, Protoss.Probe)
    once(Protoss.Dragoon)
    once(17, Protoss.Probe)
    once(Protoss.DragoonRange)
    once(19, Protoss.Probe)
    once(2, Protoss.Dragoon)
    once(20, Protoss.Probe)
    once(3, Protoss.Pylon)
    once(21, Protoss.Probe)
    get(4, Protoss.Gateway, new PlacementQuery(Protoss.Gateway).preferLabelYes(Proxy4Gate))
    once(3, Protoss.Dragoon)
    once(4, Protoss.Pylon)
    once(7, Protoss.Dragoon)
    once(5, Protoss.Pylon)
    once(11, Protoss.Dragoon)
    openingComplete ||= unitsEver(Protoss.Dragoon) >= 11
  }

}
