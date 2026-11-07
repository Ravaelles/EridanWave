package Gameplans.Protoss.PvP

import Lifecycle.With
import ProxyBwapi.Races.Protoss
import Strategery._
import Strategery.Strategies.Protoss._
import Utilities.Time._
import Utilities.?


/** Picks the core sequence and swaps strategies as intel arrives. */
trait PvPOpeningStrategy { this: PvPOpening =>

  def updateStrategy(): Unit = {
    PvPGateCore() // Make sure PvPGateCore is activated if selected

    /////////////////////
    // Update strategy //
    /////////////////////

    // Swap into 2-Gate
    if ( ! have(Protoss.Assimilator) && With.frame < Minutes(2)()) {
      if (enemyStrategy(With.fingerprints.proxyGateway, With.fingerprints.gasSteal, With.fingerprints.mannerPylon) && ! With.fingerprints.cannonRush()) {
        sequence = GGC
        PvP1012.swapIn()
      }
    }
    if (PvP1012()) {
      if ( ! have(Protoss.CyberneticsCore)) {
        PvP3Zealot()
        PvP5Zealot()
        if (enemyStrategy(With.fingerprints.proxyGateway, With.fingerprints.twoGate99, With.fingerprints.nexusFirst, With.fingerprints.gasSteal)) {
          PvP5Zealot.swapIn()
          PvP3Zealot.swapOut()
        }
      }
      if ( ! haveEverComplete(Protoss.CyberneticsCore)) {
        sevenZealot = PvP5Zealot()
        sevenZealot &&= enemyStrategy(With.fingerprints.proxyGateway, With.fingerprints.twoGate, With.fingerprints.nexusFirst)
      }
    } else {
      if ( ! have(Protoss.CyberneticsCore)) {
        zBeforeCore = enemyRecentStrategy(With.fingerprints.twoGate99, With.fingerprints.proxyGateway, With.fingerprints.mannerPylon, With.fingerprints.gasSteal, With.fingerprints.cannonRush)
        zBeforeCore &&= ! enemyStrategy(With.fingerprints.forgeFe, With.fingerprints.oneGateCore)
        zBeforeCore ||= With.fingerprints.twoGate()
        zBeforeCore ||= gcgate
        // AIIDE 2026: Trying disabling this
        zBeforeCore = false
      }
      if ( ! haveEverComplete(Protoss.CyberneticsCore)) {
        zAfterCore = true
        zAfterCore &&= ! enemyStrategy(With.fingerprints.forgeFe, With.fingerprints.coreBeforeZ)
        zAfterCore ||= enemyStrategy(With.fingerprints.mannerPylon, With.fingerprints.gasSteal, With.fingerprints.cannonRush)
        zAfterCore ||= gcgate || gctech || PvPDT()
      }
      if (units(Protoss.Gateway) < 2 && ! have(Protoss.RoboticsFacility, Protoss.CitadelOfAdun) && ! anyUpgradeStarted(Protoss.DragoonRange, Protoss.AirDamage)) {
        if (With.fingerprints.twoGate() || With.fingerprints.proxyGateway() || With.fingerprints.nexusFirst()) {
          sequence = GCGate
        } else if (
          With.fingerprints.cannonRush()
          || (With.fingerprints.earlyForge() && With.fingerprints.cannonRush.recently)
          || (With.fingerprints.rampBlock() && roll("ReactToRampBlock", 0.6))) {
          sequence = GCTech
          PvPRobo.swapIn()
          PvPReaver.swapIn()
          PvPDT.swapOut()
          PvP3GateGoon.swapOut()
          PvP4GateGoon.swapOut()
          PvPCoreExpand.swapOut()
        } else if (gcgate && enemyStrategy(With.fingerprints.oneGateCore) && ! With.strategy.isInverted) {
          sequence = ?(With.strategy.isRamped && (PvPRobo() || PvPDT()), GCTech, GCRange)
        }
      }
    }
  }

  def updateTechSwitch(): Unit = {
    // Tech switch
    // Randomly switch the learning-ordained tech based on intel and opponent tendencies, to force the opponent to play a diverse set of strategies.
    //
    // https://tl.net/forum/bw-strategy/526298-pvp-common-builds-and-what-counters-it has
    // good details on the rock-paper-scissors metagame.

    // Goon+Obs is the strongest punishment against badly hidden DT openers.
    // A glimpse of Citadel doesn't sufficiently justify switching into Obs for its own sake,
    // as the Citadel could be a fake and the investment is a lot less than making even one Observer,
    // but a switch into full-blown Robotics at least lets us benefit from the investment if the Citadel was a fake
    if (employing(PvPCoreExpand, PvP3GateGoon, PvP4GateGoon) && enemiesHave(Protoss.CitadelOfAdun) && units(Protoss.Gateway) < 3) {
      if (roll("SwapGateIntoRobo", 0.3)) {
        PvPCoreExpand.swapOut()
        PvP3GateGoon.swapOut()
        PvP4GateGoon.swapOut()
        PvPRobo.swapIn()
        PvPObs.swapIn()
      }
    }

    // Consider swapping out of DT if we've been caught, or if we scout them going DT first
    if (PvPDT() && scoutCleared && ! haveEverComplete(Protoss.Dragoon)) {
      val caught            = With.units.ours.filter(u => u.isAny(Protoss.CitadelOfAdun, Protoss.TemplarArchives) && u.knownToOpponents && ! u.visibleToOpponents).toVector
      val archivesComplete  = caught.exists(c => Protoss.TemplarArchives(c) &&    c.complete)
      val archivesStarted   = caught.exists(c => Protoss.TemplarArchives(c) &&  ! c.complete)
      val citadelComplete   = caught.exists(c => Protoss.CitadelOfAdun(c)   &&    c.complete)
      val maybeMirror       = (enemyDarkTemplarLikely || enemyRecentStrategy(With.fingerprints.dtRush)) &&  ! With.fingerprints.robo()
      val mirrorMultiplier  = ?(maybeMirror, 0.25, 1.0)
      var swapOutOfDT       = false
      if (archivesComplete) {
        // We've come too far; no point switching
      } else if (archivesStarted) {
        swapOutOfDT = roll("SwapFromDT", 0.65 * mirrorMultiplier)
      } else if (citadelComplete) {
        swapOutOfDT = roll("SwapFromDT", 0.75 * mirrorMultiplier)
      } else if (caught.nonEmpty) {
        swapOutOfDT = roll("SwapFromDT", 0.85 * mirrorMultiplier)
      } else if (maybeMirror && enemyDarkTemplarLikely && ! haveComplete(Protoss.CitadelOfAdun) && ! have(Protoss.Forge)) {
        swapOutOfDT = roll("SwapFromDT", 0.75)
      }
      if (swapOutOfDT) {
        PvPDT.swapOut()
        if (maybeMirror && roll("DTToRobo", 0.6)) {
          PvPRobo.swapIn()
          PvPObs.swapIn()
        } else if ( ! citadelComplete && roll("DTToFE", 0.6)) {
          PvPCoreExpand.swapIn()
        } else if (roll("DTTo4Gate", 0.4)) {
          PvP4GateGoon.swapIn()
        } else {
          PvP3GateGoon.swapIn()
        }
      }
    }
    if ( ! PvPDT()) {
      cancel(Protoss.CitadelOfAdun, Protoss.TemplarArchives)
      if (enemiesHave(Protoss.Observer, Protoss.Observatory)) {
        cancel(Protoss.DarkTemplar)
      }
    }
  }
}
