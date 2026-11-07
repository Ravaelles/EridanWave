package Gameplans.Protoss.PvP

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitClasses.UnitClass
import Strategery.Strategies.Protoss._
import Strategery._
import Utilities.Time._
import Utilities.UnitFilters.{IsAll, IsComplete, IsWarrior, IsWorker}
import Utilities.?


/** Mid-opening assessment: tech switches, attack timings, expand/hold/aggression flags. */
trait PvPOpeningDecisions { this: PvPOpening =>

  def updateDecisions(): Unit = {
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

    had2GateBeforeCore  ||= units(Protoss.Gateway) >= 2 && ! have(Protoss.CyberneticsCore)
    hadTechBeforeRange  ||= have(Protoss.RoboticsFacility, Protoss.CitadelOfAdun) && ! upgradeStarted(Protoss.DragoonRange)
    rangeDelayed        =   ! Protoss.DragoonRange() && (Maff.fromBoolean(With.fingerprints.twoGate() || With.fingerprints.nexusFirst()) - Maff.fromBoolean(had2GateBeforeCore) - Maff.fromBoolean(hadTechBeforeRange) > 0) // TODO: Count enemy tech before range
    timingVs2Gate       ||= With.fingerprints.twoGate() && ! enemyHasShown(Protoss.Dragoon) && ! had2GateBeforeCore && haveComplete(Protoss.Dragoon)
    timingVsProxy       ||= With.fingerprints.proxyGateway() && unitsComplete(IsWarrior) > 5

    initialZealotAttack = zBeforeCore && PvPIdeas.attackFirstZealot && haveComplete(Protoss.Zealot) && safePushing && ! enemyHasUpgrade(Protoss.DragoonRange)

    // Identify when we reach an attack timing
    atEarlyTiming ||= PvP1012()                             &&  unitsComplete(Protoss.Zealot)  >= 3
    atEarlyTiming ||= gcgate                                &&  unitsComplete(Protoss.Dragoon) > enemies(Protoss.Dragoon)
    atEarlyTiming ||= With.fingerprints.proxyGateway()      &&  unitsComplete(Protoss.Dragoon) > 0
    atEarlyTiming ||= PvPDT()                               &&  upgradeStarted(Protoss.DragoonRange)  && safePushing && ! enemyStrategy(With.fingerprints.dragoonRange, With.fingerprints.twoGateGoon, With.fingerprints.threeGateGoon, With.fingerprints.fourGateGoon, With.fingerprints.robo) // Attack if we're not clear on their plan to suss out DT mirror
    atEarlyTiming ||= Protoss.DragoonRange()                &&  enemyDarkTemplarLikely                && safePushing
    atMainTiming  ||= PvP3GateGoon()                        &&  Protoss.DragoonRange()  && unitsCompleteFor(Protoss.Dragoon.buildFrames, Protoss.Gateway) >= 3 && unitsComplete(IsWarrior) >= 6
    atMainTiming  ||= PvP4GateGoon()                        &&  Protoss.DragoonRange()  && unitsCompleteFor(Protoss.Dragoon.buildFrames, Protoss.Gateway) >= 4 && unitsComplete(IsWarrior) >= 7
    atMainTiming  ||= PvPRobo()                             &&  Protoss.DragoonRange()  && unitsComplete(Protoss.Reaver) * unitsComplete(Protoss.Shuttle) >= 2 && unitsComplete(IsWarrior) >= 8
    atMainTiming  ||= PvPDT()                               &&  haveComplete(Protoss.DarkTemplar)
    atMainTiming  ||= speedlotAllIn                         &&  Protoss.ZealotSpeed()   && (haveComplete(Protoss.Observer) || ! enemiesHave(Protoss.DarkTemplar))
    atMainTiming  ||= With.fingerprints.dtRush()            &&  haveComplete(Protoss.Observer)
    atMainTiming  ||= With.fingerprints.cannonRush()        &&  haveComplete(Protoss.Reaver)
    atMainTiming  ||= enemyStrategy(With.fingerprints.forgeFe, With.fingerprints.cannonRush, With.fingerprints.nexusFirst, With.fingerprints.rampBlock)
    atMainTiming  ||= enemyBases > 1
    atMainTiming  ||= With.fingerprints.cannonRush.recently && ! With.fingerprints.gatewayFirst()

    // There may not be a timing depending on what our opponent does,
    // or the timing window might close permanently.
    earlyTimingClosed   ||= PvP1012()         && With.fingerprints.twoGate()
    earlyTimingClosed   ||= PvP1012()         && (enemyHasUpgrade(Protoss.DragoonRange) || With.fingerprints.oneGateCore()) && With.frame > GameTime(5, 10)() && ! Protoss.DragoonRange()
    earlyTimingClosed   ||= gcgate            && enemyHasUpgrade(Protoss.DragoonRange) && ! safePushing && enemyStrategy(With.fingerprints.twoGateGoon, With.fingerprints.threeGateGoon, With.fingerprints.fourGateGoon)
    mainTimingClosed    ||= PvP3GateGoon()    && enemyStrategy(With.fingerprints.fourGateGoon, With.fingerprints.threeGateGoon)
    mainTimingClosed    ||= PvP4GateGoon()    && With.fingerprints.fourGateGoon()
    mainTimingClosed    ||= PvPDT()           && enemiesHaveComplete(Protoss.Observer, Protoss.PhotonCannon) && unitsComplete(Protoss.Gateway) < 3
    mainTimingClosed    &&= ! reaverAllIn
    mainTimingClosed    &&= ! speedlotAllIn
    shouldAttack          = initialZealotAttack
    shouldAttack        ||= atEarlyTiming && ! earlyTimingClosed
    shouldAttack        ||= atMainTiming && ! mainTimingClosed
    shouldAttack        ||= timingVs2Gate
    shouldAttack        ||= timingVsProxy
    shouldAttack        &&= safePushing
    shouldAttack        ||= timingVs2Gate
    shouldAttack        &&= ! enemiesHave(Protoss.DarkTemplar) || unitsComplete(Protoss.Observer) > 1
    shouldAttack        ||= With.units.ours.exists(_.agent.commit) && With.frame < Minutes(5)() // Ensure that committed Zealots keep wanting to attack
    shouldHarass          = upgradeStarted(Protoss.ShuttleSpeed) && unitsComplete(Protoss.Reaver) > 1
  }
}
