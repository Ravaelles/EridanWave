package Gameplans.Protoss.PvP.opening

import Gameplans.Protoss.PvP.PvPOpening
import Gameplans.Protoss.PvP.PvPIdeas
import Lifecycle.With
import Utilities.?

/**
 * Status reporting: mirrors the timing and tech flags onto the status line
 * and issues the resulting attack/harass/hold intents. Read-only on game
 * state apart from the intents themselves.
 */
trait PvPOpeningStatus { self: PvPOpening =>

  private[PvP] def reportStatus(): Unit = {
    /////////////
    // Logging //
    /////////////

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

  private def holdFoyer(): Unit = {
    With.blackboard.basesToHold.set(Vector(With.geography.ourFoyer))
  }
}
