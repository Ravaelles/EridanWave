package Gameplans.Protoss.PvP.opening

import Gameplans.Protoss.PvP.PvPOpening
import Lifecycle.With
import ProxyBwapi.Races.Protoss
import Strategery.Strategies.Protoss._
import Utilities.?
import Utilities.Time.{GameTime, Minutes}

/**
 * Reactions to enemy intel: strategy swaps (2-Gate, cannon rush, DTraft)
 * and per-tech option flags. Reads fingerprints, writes state and the
 * selected branch via swapIn/swapOut.
 */
trait PvPOpeningReactions { self: PvPOpening =>

  private[PvP] def updateStrategySwaps(): Unit = {
    // Swap into 2-Gate
    if ( ! have(Protoss.Assimilator) && With.frame < Minutes(2)()) {
      if (enemyStrategy(With.fingerprints.proxyGateway, With.fingerprints.gasSteal, With.fingerprints.mannerPylon) && ! With.fingerprints.cannonRush()) {
        sequence = GGC
        PvP1012.swapIn()
      }
    }
    if (PvP1012()) {
      if ( ! have(Protoss.Assimilator)) {
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

    /////////////////
    // Tech switch //
    /////////////////

    // Randomly switch the learning-ordained tech based on intel and opponent tendencies,
    // to augment learning and force the opponent to play a diverse set of strategies.
    //
    // https://tl.net/forum/bw-strategy/526298-pvp-common-builds-and-what-counters-it-t-l
    // has some good details on the metagame rock-paper-scissors.

    // Goon+Obs is the strongest punishment against badly hidden DT openers.
    // A glimpse of Citadel doesn't sufficiently justify switching into Obs for its own sake,
    // as the Citadel could be a fake and the investment is a lot less than making even one Observer,
    // but a switch into full-blown Robotics at least lets us benefit from the investment if the Citadel was a fake
    if (employing(PvPCoreExpand, PvP3GateGoon, PvP4GateGoon) && enemiesHave(Protoss.CitadelOfAdun, Protoss.TemplarArchives) && units(Protoss.Gateway) < 3) {
      if (roll("SwapGateIntoRoboVsCitadel", 0.3)) {
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

  private[PvP] def decideTechOptions(): Unit = {
    /////////////////////////////
    // Tech-specific decisions //
    /////////////////////////////

    if (PvPRobo()) {
      val enemyCitadel  = enemyDarkTemplarLikely || enemiesHave(Protoss.CitadelOfAdun)
      // TODO: Also all-in vs. 1 Gate Core Expand
      reaverAllIn     ||= enemyStrategy(With.fingerprints.nexusFirst, With.fingerprints.forgeFe, With.fingerprints.gatewayFe, With.fingerprints.dtRush)
      reaverAllIn     ||= With.fingerprints.twoGate() && enemyBases > 1
      reaverAllIn     ||= With.fingerprints.twoGate() && (enemyHasShown(Protoss.CitadelOfAdun) || enemyHasUpgrade(Protoss.ZealotSpeed))
      getReavers        = PvPReaver()
      getReavers      ||= reaverAllIn
      getReavers      ||= have(Protoss.RoboticsSupportBay, Protoss.Reaver, Protoss.Shuttle)
      getReavers      ||= enemyStrategy(With.fingerprints.fourGateGoon)

      // Look for reasons to avoid making an Observer.
      // It's okay to switch even if we already started an Observatory or Observers,
      // because we can cancel and switch out of them at any time.

      getObservatory    = true
      getObservers      = true
      if (With.fingerprints.dtRush()) { // Yep, get those Observers
      } else if (shuttleSpeed) { // This strategy demands a ton of gas; we can't afford the Observer
        getObservatory  = false
        getObservers    = false
      } else if (enemyStrategy(With.fingerprints.nexusFirst, With.fingerprints.robo, With.fingerprints.threeGateGoon, With.fingerprints.fourGateGoon)) { // We have all the intel we need to act, and can rule out DT
        getObservatory  = false
        getObservers    = false
      } else if (PvPObs()) { // Obs is what we're here for!
      } else if ( ! With.fingerprints.dragoonRange() && With.units.enemy.exists(e => Protoss.Dragoon(e) && e.lastSeen > GameTime(5, 15)())) { // If Dragoon range is supiciously absent we should prepare for DT
      } else if (enemyRecentStrategy(With.fingerprints.dtRush)) { // Don't overthink it
      } else {
        var p = 1.0
        p *= ?(enemyRecentStrategy(With.fingerprints.fourGateGoon, With.fingerprints.threeGateGoon, With.fingerprints.robo),  0.6, 1.0)
        p *= ?(trackRecordLacks(With.fingerprints.dtRush),                                                                    0.6, 1.0)
        getObservatory  =                   roll("SpeculativeObservatory",  p)
        getObservers    = getObservatory && roll("SpeculativeObservers",   p) // So the probability of obs is the *joint* probability
      }
      val dtSmell = enemyCitadel || enemyHasShown(Protoss.Forge, Protoss.PhotonCannon) || With.sense.netDamage > 2 * Protoss.Dragoon.subjectiveValue
      getObservatory  ||= dtSmell
      getObservers    ||= dtSmell
      getReavers      ||= ! getObservatory && ! have(Protoss.Observatory)
      if ( ! have(Protoss.Shuttle, Protoss.RoboticsSupportBay, Protoss.Observatory)) {
        shuttleFirst = getReavers && ! enemyCitadel && ! With.fingerprints.cannonRush()
      }
      shuttleSpeed = shuttleFirst && gctech && ! getObservatory && ! getObservers && ! have(Protoss.Observatory, Protoss.Observer) && roll("ShuttleSpeedRush", 0.0)
    } else if (PvPDT()) {
      greedyDT    = have(Protoss.TemplarArchives)
      greedyDT  &&= ! enemyStrategy(With.fingerprints.twoGate, With.fingerprints.dtRush)
      greedyDT  &&= roll("DTGreedyExpand", ?(enemyRecentStrategy(With.fingerprints.dtRush), 0.0, 0.5))

      if (have(Protoss.CitadelOfAdun) && have(Protoss.RoboticsFacility) && bases < 2) {
        speedlotAllIn = true
      }
    }
  }
}
