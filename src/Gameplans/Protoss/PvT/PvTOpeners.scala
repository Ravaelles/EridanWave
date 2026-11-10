package Gameplans.Protoss.PvT

import Gameplans.All.GameplanImperative
import Lifecycle.With
import Macro.Requests.RequestUnit
import Placement.Access.PlaceLabels.Proxy4Gate
import Placement.Access.{PlaceLabels, PlacementQuery}
import ProxyBwapi.Races.{Protoss, Terran}
import Strategery.Strategies.Protoss._
import Utilities.?
import Utilities.Time.{GameTime, Minutes}
import Utilities.UnitFilters.{IsAll, IsProxied, IsWarrior, IsWorker}

abstract class PvTOpeners extends GameplanImperative
  with openings.PvTOpenOpen12NexusGateScout
  with openings.PvTOpenOpenZealotExpand
  with openings.PvTOpenOpenZZCoreZ
  with openings.PvTOpenOpen20Rangeless
  with openings.PvTOpenOpen28Nexus
  with openings.PvTOpenOpen910
  with openings.PvTOpenOpen1015
  with openings.PvTOpenOpenReaver
  with openings.PvTOpenOpen29Arbiter
  with openings.PvTOpenOpenDT
  with openings.PvTOpenOpenGasSteal
  with openings.PvTOpenOpen4Gate {

  var openingComplete: Boolean = false

  override def doWorkers(): Unit = pumpWorkers(oversaturate = true, maximumTotal = 70)

  override def executeBuild(): Unit = {
    open()
    PvTFerry.ensureFerry()
  }

  def open(): Unit = {
    if (openingComplete) return

    doOpeningShortCircuits()
          if (PvT13Nexus())       open12NexusGateScout()
    else  if (PvTZealotExpand())  openZealotExpand()
    else  if (PvTZZCoreZ())       openZZCoreZ()
    else  if (PvTRangeless())     open20Rangeless()
    else  if (PvT28Nexus())       open28Nexus()
    else  if (PvT910())           open910()
    else  if (PvT1015())          open1015()
    else  if (PvT4Gate())         open4Gate()
    else  if (PvT1BaseReaver())   openReaver()
    else  if (PvT29Arbiter())     open29Arbiter()
    else  if (PvTDT())            openDT()
    else  if (PvTGasSteal())      openGasSteal()
    else                          openZZCoreZ() // A safe default if something went wrong
  }


  def openGateCore(): Unit = {
    once(8, Protoss.Probe)
    once(Protoss.Pylon)
    once(10, Protoss.Probe)
    once(Protoss.Gateway)
    once(12, Protoss.Probe)
    once(Protoss.Assimilator)
    once(13, Protoss.Probe)
    doOpeningReactions()
    once(Protoss.CyberneticsCore)
  }


  def doOpeningShortCircuits(): Unit = {
    if (PvT28Nexus() && enemyStrategy(With.fingerprints.fourteenCC, With.fingerprints.oneRaxFE)) {
      status("AcceleratedExpand")
      requireMiningBases(2)
    }
    if (PvTZZCoreZ() && enemyStrategy(With.fingerprints.wallIn)) {
      PvTZZCoreZ.swapOut()
      PvTZealotExpand.swapIn()
    }
  }

  def barracksCheese: Boolean = enemyStrategy(With.fingerprints.fiveRax, With.fingerprints.proxyRax, With.fingerprints.bbs, With.fingerprints.twoRax1113, With.fingerprints.twoRaxAcad) && ! enemyHasShown(Terran.Vulture, Terran.SiegeTankUnsieged, Terran.SiegeTankSieged)

  def doOpeningReactions(): Unit = {
    // Vs. BBS: Zealots into 3-Gate Goon into Reaver (And expand after Reaver if they do)
    // Vs. 11-13: Zealots into 3-Gate Goon into Reaver (And expand after Reaver if they do)
    // Vs. Worker rush: Zealots into 3-Gate Goon
    // Vs. 2-Fac, or possibly any scout-blocking wall-in: Gate-Core-Gate before expand

    if (frame < Minutes(8)() && With.fingerprints.workerRush() && enemies(IsWarrior) == 0) {
      status("ReactToWorkerRush")
      pump(Protoss.Probe, 9)
      if (units(IsWarrior) <= 3) cancel(Protoss.Nexus)
      gasLimitCeiling(50)
            if (units(IsWorker) >= 10)  gasWorkerCeiling(2)
      else  if (units(IsWorker) >= 7)   gasWorkerCeiling(1)
      else                              gasWorkerCeiling(0)
      pump(Protoss.Dragoon, 3)
      pump(Protoss.Zealot, 3)
      once(8, Protoss.Probe)
      once(Protoss.Pylon)
      once(10, Protoss.Probe)
      once(Protoss.Gateway)
      once(12, Protoss.Probe)
      pumpWorkers()
      once(Protoss.Assimilator, Protoss.CyberneticsCore)
      once(2, Protoss.Pylon)
      once(2, Protoss.Gateway)
      once(Protoss.DragoonRange)
    }
    if (frame < Minutes(7)() && With.fingerprints.bunkerRush() && (enemies(IsAll(Terran.Bunker, IsProxied), Terran.Marine) > 0)) {
      status("ReactToBunkerRush")
      pump(Protoss.Dragoon, 3)
      if ( ! haveComplete(Protoss.CyberneticsCore)) {
        pump(Protoss.Zealot, 3)
      }
    }
    if (frame < Minutes(10)() && PvT13Nexus() && With.geography.ourNatural.enemies.exists(e => Terran.Bunker(e) && e.complete)) {
      cancel(Protoss.Nexus)
      get(Protoss.DragoonRange)
      pump(Protoss.Dragoon)
      get(3, Protoss.Gateway)
      gasLimitCeiling(250)
      get(Protoss.RoboticsFacility)
      if (Protoss.DragoonRange()) {
        attack()
      }
      return
    }

    if (frame < Minutes(10)() && barracksCheese) {
      status("ReactToRaxCheese")
      gasLimitCeiling(200)
      once(8, Protoss.Probe)
      once(Protoss.Pylon)
      once(10, Protoss.Probe)
      once(Protoss.Gateway)
      once(13, Protoss.Probe)
      once(Protoss.Zealot)


      if (enemyStrategy(With.fingerprints.fiveRax, With.fingerprints.proxyRax, With.fingerprints.bbs) && units(Protoss.Gateway) < 2) {
        gasWorkerCeiling(0)
        cancel(Protoss.Assimilator, Protoss.CyberneticsCore)
      }

      autosupply()
      pumpShuttleAndReavers(2, shuttleFirst = false)
      pump(Protoss.Zealot, 3)
      pumpWorkers()
      if (haveComplete(Protoss.Reaver)) {
        requireMiningBases(2)
      } else {
        cancel(Protoss.Nexus, Protoss.CitadelOfAdun, Protoss.TemplarArchives, Protoss.Stargate)
      }
      if (upgradeComplete(Protoss.DragoonRange, 1, Protoss.Dragoon.buildFrames)) {
        pump(Protoss.Dragoon)
      }
      pump(Protoss.Zealot, 7)
      get(2, Protoss.Gateway)
      get(Protoss.Assimilator)
      get(Protoss.CyberneticsCore)
      get(Protoss.DragoonRange)
      pump(Protoss.Dragoon)
      get(Protoss.RoboticsFacility)
      get(Protoss.RoboticsSupportBay)
      once(3, Protoss.Gateway)
    }
  }
}
