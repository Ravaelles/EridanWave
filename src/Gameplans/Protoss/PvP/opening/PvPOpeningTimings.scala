package Gameplans.Protoss.PvP.opening

import Gameplans.Protoss.PvP.PvPOpening
import Gameplans.Protoss.PvP.PvPIdeas
import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.Races.Protoss
import Strategery.Strategies.Protoss._
import Utilities.?
import Utilities.Time.{GameTime, Minutes}
import Utilities.UnitFilters.{IsAll, IsComplete, IsWarrior}

/**
 * Attack timing and expansion decisions: when our window opens, when it
 * closes, and how aggressive the squads may be. Writes the should* flags
 * and the blackboard aggression level. No build orders here.
 */
trait PvPOpeningTimings { self: PvPOpening =>

  private[PvP] def updateTimings(): Unit = {
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
    shouldAttack        ||= atEarlyTiming  && ! earlyTimingClosed
    shouldAttack        ||= atMainTiming   && ! mainTimingClosed
    shouldAttack        ||= timingVsProxy
    shouldAttack        &&= safePushing
    shouldAttack        ||= timingVs2Gate
    shouldAttack        &&= ! enemiesHave(Protoss.DarkTemplar) || unitsComplete(Protoss.Observer) > 1
    shouldAttack        ||= With.units.ours.exists(_.agent.commit) && With.frame < Minutes(5)() // Ensure that committed Zealots keep wanting to attack
    shouldHarass          = upgradeStarted(Protoss.ShuttleSpeed) && unitsComplete(Protoss.Reaver) > 1

    if (reaverAllIn) {
      shouldExpand = units(Protoss.Reaver) >= 2 && units(Protoss.Shuttle) >= 1
    } else if (PvPRobo()) {
      shouldExpand    = haveComplete(Protoss.Reaver)
      shouldExpand  ||= ! getReavers && unitsComplete(IsWarrior) >= ?(enemyBases > 1, 6, 12)
      shouldExpand  ||= enemyRobo
      if ( ! PvPReaver()) {
        shouldExpand ||= enemyBases > 1
        shouldExpand ||= units(Protoss.Observer) >= 2 && unitsComplete(IsWarrior) >= 9
      }
      shouldExpand  &&= PvPIdeas.pvpSafeAtHome
      shouldExpand  &&= ! shuttleSpeed
      shouldExpand  &&= Protoss.DragoonRange()
      shouldExpand  ||= unitsComplete(Protoss.Reaver) > 1
    } else if (PvPDT()) {
      shouldExpand    = atMainTiming
      shouldExpand  ||= have(Protoss.DarkTemplar) && (PvPIdeas.pvpSafeAtHome || With.scouting.enemyProximity < 0.75)
      shouldExpand  ||= greedyDT
    } else if (PvP3GateGoon()) {
      shouldExpand    = atMainTiming
      shouldExpand  &&= units(IsWarrior) >= 9
      shouldExpand  &&= PvPIdeas.pvpSafeAtHome
    } else if (PvP4GateGoon()) {
      shouldExpand    = atMainTiming && ! mainTimingClosed && unitsEver(IsWarrior) >= 20
      shouldExpand  ||= unitsComplete(IsWarrior) >= ?(safePushing, ?(PvPIdeas.enemyContained, 14, 20), 28)
    }
    shouldExpand  ||= enemyStrategy(With.fingerprints.forgeFe, With.fingerprints.gatewayFe) && ! With.fingerprints.cannonRush()
    shouldExpand  ||= unitsComplete(IsWarrior) >= 30 // We will get contained if we wait too long

    // If we want to expand, make sure we control our natural
    shouldHoldNatural   = safeDefending
    shouldHoldNatural &&= enemyBases > 1 || ! PvPDT()    || unitsComplete(Protoss.DarkTemplar) > 0
    shouldHoldNatural &&= enemyBases > 1 || ! getReavers || unitsComplete(Protoss.Reaver) > 0
    shouldHoldNatural &&= ! rangeDelayed || Protoss.DragoonRange()
    shouldHoldNatural &&= enemyHasShown(Protoss.Dragoon)
    shouldHoldNatural ||= shouldAttack
    shouldHoldNatural ||= shouldExpand && bases < 2

    // Chill vs. 2-Gate until we're ready to defend
    if ( ! PvP1012() && With.fingerprints.twoGate() && unitsEver(IsAll(Protoss.Dragoon, IsComplete)) == 0) {
      aggression(0.6)
    } else if (atMainTiming) {
      // Amp up aggression to ensure we can get down our ramp
      aggression(1.0 + 0.25 * unitsComplete(Protoss.Reaver))
    }
  }
}
