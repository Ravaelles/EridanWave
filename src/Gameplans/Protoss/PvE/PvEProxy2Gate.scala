package Gameplans.Protoss.PvE

import Gameplans.All.GameplanImperative
import Lifecycle.With
import Placement.Access.PlaceLabels._
import Placement.Access.PlacementQuery
import ProxyBwapi.Races.{Protoss, Zerg}
import Strategery.Strategies.Protoss.PvEProxy2Gate
import Utilities.?

class PvEProxy2Gate extends GameplanImperative {

  override def activated: Boolean = PvEProxy2Gate()
  override def completed: Boolean = bases > 1

  override def executeBuild(): Unit = {
    scoutOn(Protoss.Gateway, quantity = 2)

    once(8, Protoss.Probe)
    val enemyMains = With.geography.mains.filterNot(_.isOurMain)
    if ( ! haveEver(Protoss.Pylon)) {
      get(1, Protoss.Pylon, new PlacementQuery(Protoss.Pylon)
        .requireLabelYes(Proxy2Gate)
        .requireZone(With.geography.zones.filter(_.heart.groundTiles(With.geography.home) > 40): _*)
        .preferBase(enemyMains: _*)
        .preferZone(With.geography.mains.filterNot(_.isOurMain).map(_.zone): _*))
    }
    once(9, Protoss.Probe)
    get(2, Protoss.Gateway)
    once(11, Protoss.Probe)
    once(1, Protoss.Zealot)
    once(2, Protoss.Zealot)
    get(2, Protoss.Pylon)
    once(3, Protoss.Zealot)
    once(12, Protoss.Probe)
  }

  doAutosupply = false

  override def doWorkers(): Unit = {}

  override def executeMain(): Unit = {
    val strength = With.units.playerOwned
      .filter(u => u.complete && u.unitClass.isWarrior)
      .map(u => u.unitClass.mineralPrice * u.totalHealth / u.unitClass.maxTotalHealth.toDouble * ?(u.isFriendly, 1.5, -1))
      .sum
    if (strength > 0) {
      attack()
    }

    val unpoweredGateways = With.units.ours.count(u => Protoss.Gateway(u) && ! u.powered)
    requireMiningBases(1)
    if (have(Protoss.CyberneticsCore)) {
      autosupply()
      pumpWorkers()
      get(Protoss.DragoonRange)
      pump(Protoss.DarkTemplar)
      pump(Protoss.Dragoon)
      get(3 + unpoweredGateways, Protoss.Gateway)
      get(Protoss.CitadelOfAdun)
      get(Protoss.TemplarArchives)
      pump(Protoss.Zealot)
    } else {
      if (With.enemy.isZerg) {
        autosupply()
        pump(Protoss.Zealot)
        pumpWorkers()
        get(6, Protoss.Gateway)
        // Failed proxy with a standing army is a tech transition, not a
        // plan change: past thirteen Zealots, or the moment Hydralisks show
        // (Zealots cannot trade into range), gas up so the Dragoon line
        // (already prioritized) can take over.
        if (units(Protoss.Zealot) > 13 || enemyHasShown(Zerg.Hydralisk)) {
          get(1, Protoss.Assimilator, new PlacementQuery(Protoss.Assimilator).requireBase(With.geography.ourMain))
          get(Protoss.CyberneticsCore)
          get(Protoss.DragoonRange)
        }
      } else if (With.enemy.isProtoss) {
        autosupply()
        pump(Protoss.Zealot)
        pumpWorkers()
        get(1, Protoss.Assimilator, new PlacementQuery(Protoss.Assimilator).requireBase(With.geography.ourMain))
        get(Protoss.CyberneticsCore)
      } else {
        once(5, Protoss.Zealot)
        once(3, Protoss.Pylon)
        once(13, Protoss.Probe)
        get(1, Protoss.Assimilator, new PlacementQuery(Protoss.Assimilator).requireBase(With.geography.ourMain))
        once(14, Protoss.Probe)
        get(Protoss.CyberneticsCore)
        once(15, Protoss.Probe)
        get(3, Protoss.Gateway)
        autosupply()
        once(16, Protoss.Probe)
        pump(Protoss.Zealot)
        pumpWorkers()
      }
    }
    requireMiningBases(2)
  }

  def slushOrPylons(): Unit = {
    if (With.tactics.workerScout.scoutingAbandoned || ! enemyIsProtoss) {
      autosupply()

      return
    }

    if (With.tactics.workerScout.scouts.exists(_.base.exists(_.isEnemy))) return microSlush(100)

    autosupply()
  }
}


