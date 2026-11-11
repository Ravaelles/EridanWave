package Gameplans.Protoss.PvT.openings

import Lifecycle.With
import Macro.Requests.RequestUnit
import Placement.Access.{PlaceLabels, PlacementQuery}
import ProxyBwapi.Races.Protoss
import Utilities.Time.{GameTime, Minutes}

/** One PvT opening recipe; mixed into PvTOpeners. Verbatim move. */
trait PvTOpenOpenGasSteal { self: Gameplans.Protoss.PvT.PvTOpeners =>

  def openGasSteal(): Unit = {
    // Rain vs. Sharp: https://youtu.be/IckWDksioQE?t=1293
    // 10 Gas Steal 11 Gate
    // Mini vs. Organ https://youtu.be/Cl2MHjmGBLk?t=1342
    // 10 Gate 12 Gas Steal
    val buildInNatural = With.placement.wall.isDefined && With.frame < GameTime(2, 0)()

    scoutOn(Protoss.Pylon)

    once(8, Protoss.Probe)
    if (buildInNatural) {
      get(1,
        Protoss.Pylon,
        new PlacementQuery(Protoss.Pylon)
          .preferTile()
          .preferLabelNo()
          .preferLabelYes(PlaceLabels.Wall)
          .preferZone(With.geography.ourFoyer.edges.flatMap(_.zones).distinct: _*))
    } else {
      once(Protoss.Pylon)
    }

    once(10, Protoss.Probe)
    once(Protoss.Gateway)
    once(11, Protoss.Probe)

    val scout = With.tactics.workerScout.lock.units.headOption
    val enemyMain = With.scouting.enemyMain
    if (scout.isDefined && enemyMain.exists(_.gas.forall(_.isNeutral)) && With.frame < GameTime(2, 5)()) {
      get(RequestUnit(Protoss.Assimilator, 1, placementQueryArg = Some(new PlacementQuery(Protoss.Assimilator).requireBase(enemyMain.get))))
    }
    if (With.fingerprints.wallIn() && safeDefending && With.frame < Minutes(4)()) {
      cancel(Protoss.Zealot)
    }

    once(13, Protoss.Probe)
    once(Protoss.Zealot)
    once(14, Protoss.Probe)
    once(2, Protoss.Pylon)
    once(15, Protoss.Probe)

    get(RequestUnit(Protoss.Assimilator, 1, placementQueryArg = Some(new PlacementQuery(Protoss.Assimilator).requireBase(With.geography.ourMain))))
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
    once(4, Protoss.Gateway)
    once(22, Protoss.Probe)
    once(10, Protoss.Dragoon)
    if (unitsEver(Protoss.Gateway) < 4) {
      gasWorkerCeiling(2)
    }
  }
}
