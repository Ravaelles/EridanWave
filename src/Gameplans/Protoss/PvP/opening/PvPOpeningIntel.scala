package Gameplans.Protoss.PvP

import Lifecycle.With
import ProxyBwapi.Races.Protoss
import Utilities.Time._


/** Status reporting and scouting. */
trait PvPOpeningIntel { this: PvPOpening =>

  def reportStatus(): Unit = {
    if ( ! ggcore) {
      status(
        ?(zBeforeCore,
          ?(zAfterCore, "ZCoreZ", "ZCore"),
          ?(zAfterCore, "CoreZ",  "NZCore")))
      status(sequence.toString)
    }
    status(sevenZealot,         "SevenZealots")
    status(commitZealots,       "CommitZealots")
    status(shuttleFirst,        "ShuttleFirst")
    status(shuttleSpeed,        "ShuttleSpeed")
    status(getObservers,        "Obs")
    status(getObservatory,      "Observatory")
    status(getReavers,          "Reaver")
    status(reaverAllIn,         "ReaverAllIn")
    status(greedyDT,            "GreedyDT")
    status(speedlotAllIn,       "SpeedlotAllIn")
    status(initialZealotAttack, "InitialZealotAttack")
    status(atEarlyTiming,       "Timing1")
    status(earlyTimingClosed,   "Timing1Done")
    status(atMainTiming,        "Timing2")
    status(mainTimingClosed,    "Timing2Done")
    status(timingVs2Gate,       "TimingVs2Gate")
    status(timingVsProxy,       "TimingVsProxy")
    status(shouldAttack,        "Attack")
    status(shouldHarass,        "Harass")
    status(shouldExpand,        "Expand")
    status(shouldHoldNatural,   "HoldNat")

    if (shouldAttack) { attack() }
    if (shouldHarass) { harass() }
    if (shouldHoldNatural) { holdFoyer() }
    PvPIdeas.monitorSafely()
  }

  def runScouting(): Unit = {
    if ( ! enemiesHave(Protoss.Dragoon)
      && ! With.fingerprints.proxyGateway()
      && ! With.units.enemy.filter(Protoss.CyberneticsCore).exists(With.frame - _.completionFrame > Protoss.Dragoon.buildFrames - Seconds(5)())) {
      if (starts > 3 || enemyRecentStrategy(With.fingerprints.proxyGateway, With.fingerprints.twoGate99)) {
        scoutOn(Protoss.Gateway)
      } else if ( ! zBeforeCore || ! PvPIdeas.attackFirstZealot) {
        if (PvP1012()) {
          scoutOn(Protoss.Gateway, quantity = 2)
        } else {
          scoutOn(Protoss.CyberneticsCore)
        }
      }
    }
  }
}
