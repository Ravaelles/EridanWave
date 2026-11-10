package Gameplans.Protoss.PvP.opening

import Gameplans.Protoss.PvP.PvPOpening
import Gameplans.Protoss.PvP.PvPIdeas
import Lifecycle.With
import Macro.Requests.RequestUnit
import Mathematics.Maff
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitClasses.UnitClass
import Strategery.Strategies.Protoss._
import Utilities.?
import Utilities.Time.Forever
import Utilities.UnitFilters.IsWarrior

/**
 * Mid-game tech execution: gas management, the Robo / DT / CoreExpand /
 * Gateway dispatch and the shared tail. Each run* method issues orders
 * for one tech path.
 */
trait PvPOpeningTech { self: PvPOpening =>

  private[PvP] def manageGas(): Unit = {
    if (gasCapsUntouched) {
      gasLimitCeiling(350)
      if (PvP1012()) {
        gasWorkerCeiling(3)
      } else if (zBeforeCore && units(Protoss.CyberneticsCore) < 1) {
        gasWorkerCeiling(1)
      } else if (zBeforeCore && unitsComplete(Protoss.CyberneticsCore) < 1) {
        gasWorkerCeiling(2)
      } else if (employing(PvPCoreExpand, PvP3GateGoon, PvP4GateGoon) && units(Protoss.Gateway) < ?(PvP4GateGoon(), 4, 3)) {
        gasWorkerCeiling(2)
        gasLimitCeiling(250)
      }
      if (PvPCoreExpand() && (upgradeStarted(Protoss.DragoonRange) || gas >= 200)) {
        val need = Math.max(1, units(Protoss.Gateway))
        gasWorkerCeiling(need)
        gasLimitCeiling(100 * need)
      }
    }
  }

  private[PvP] def guardTechTransition(): Unit = {
    ////////////////////////
    // Transition to tech //
    ////////////////////////

    // The build order should have requested all of these, but just in case:
    once(Protoss.Gateway, Protoss.Assimilator, Protoss.CyberneticsCore)
    if (gcgate || PvP1012()) { once(2, Protoss.Gateway) }
    once(Protoss.Dragoon)
    if ( ! gctech) { get(Protoss.DragoonRange) }
  }

  private[PvP] def runTech(): Unit = {
    //////////
    // Tech /////////////////////////////////////////////////////////////////////
    //////////

    if (PvPRobo()) {
      runRoboTech()
    } else if (PvPDT()) {
      runDTTech()
    } else if (PvPCoreExpand()) {
      runCoreExpandTech()

    ///////////////////
    // 3/4-Gate Goon //
    ///////////////////

    } else {
      runGatewayTech()
    }
  }

  private[PvP] def runRoboTech(): Unit = {
    //////////
    // Robo //
    //////////

    def minFramesUntil(unitClass: UnitClass): Int = ?(haveEverComplete(unitClass), 0, Maff.min(With.units.ours.filter(unitClass).map(_.remainingCompletionFrames)).getOrElse(unitClass.buildFrames))
    get(Protoss.RoboticsFacility)

    if (shuttleFirst) {
      once(Protoss.Shuttle)
    }

    ///////////////
    // Observers //
    ///////////////

    if (getObservers || getObservatory) {
      if (enemyDarkTemplarLikely && ! have(Protoss.Observer)) {
        if ( ! have(Protoss.Observatory)) {
          if (gas < 100) {
            cancel(Protoss.RoboticsSupportBay, Protoss.ShuttleSpeed)
          }
        } else if (have(Protoss.Observatory) && ! have(Protoss.Observer)) {
          cancel(Protoss.Shuttle, Protoss.Reaver)
        }
      }

      get(RequestUnit(Protoss.Observatory,
        minStartFrameArg =
          With.units.ours
            .find(Protoss.Shuttle)
            .map(With.frame + _.remainingCompletionFrames - Protoss.Observatory.buildFrames)
            .getOrElse(?(shuttleFirst, Forever(), 0))))

      if (getObservers) {
        if (enemyDarkTemplarLikely) {
          get(2, Protoss.Observer)
        } else {
          once(Protoss.Observer)
        }
      }
    } else {
      cancel(Protoss.Observer)
      cancel(Protoss.Observatory)
    }

    /////////////////////
    // Shuttle/Reavers //
    /////////////////////

    if (getReavers) {
      var bayStartFrame = With.frame - Protoss.RoboticsSupportBay.buildFrames
      if (shuttleFirst) {
        bayStartFrame += minFramesUntil(Protoss.Shuttle)
      }
      if (getObservatory) {
        bayStartFrame += minFramesUntil(Protoss.Observatory)
      }
      if (getObservers) {
        bayStartFrame += minFramesUntil(Protoss.Observer)
      }
      get(RequestUnit(Protoss.RoboticsSupportBay, minStartFrameArg = bayStartFrame))
    }
    if (getReavers) {
      once(Protoss.Reaver)
    }
    if (shuttleSpeed) {
      once(Protoss.ShuttleSpeed)
      once(Protoss.Shuttle)
    }

    ////////////
    // Basics //
    ////////////

    get(Protoss.DragoonRange)
    if (shouldExpand && (With.scouting.weControlOurFoyer || units(Protoss.Reaver) > 1)) {
      expand()
    }
    trainRoboUnits()
    if (reaverAllIn || enemyStrategy(With.fingerprints.twoGateGoon, With.fingerprints.threeGateGoon, With.fingerprints.fourGateGoon)) {
      get(3, Protoss.Gateway)
    } else {
      pumpWorkers(oversaturate = true) // Need to keep worker production up in case they just expand
    }
    trainGatewayUnits()
    pumpWorkers(oversaturate = true)
    get(3, Protoss.Gateway)
  }

  private[PvP] def runDTTech(): Unit = {
    ////////
    // DT //
    ////////

    if (cannonExpand) {
      expand()
    } else if ( ! enemyHasShown(Protoss.Observer, Protoss.Observatory)) {
      sneakyCitadel()
      get(Protoss.TemplarArchives)
      once(Math.min(2, unitsComplete(Protoss.Gateway)), Protoss.DarkTemplar)
    }

    if (speedlotAllIn) {
      get(Protoss.ZealotSpeed)
      pump(Protoss.Zealot)
      get(5, Protoss.Gateway)
      cancel(Protoss.DragoonRange)
    }

    if (unitsComplete(Protoss.Gateway) < 2) {
      trainGatewayUnits()
    }
    if (shouldExpand) {
      expand()
    }
    pumpWorkers(oversaturate = true)
    trainGatewayUnits()
    get(2, Protoss.Gateway)
    if (PvPIdeas.enemyDarkTemplarPossible) {
      get(Protoss.Forge)
    }
    expand()
  }

  private[PvP] def runCoreExpandTech(): Unit = {
    get(Protoss.DragoonRange)
    if (shouldExpand) {
      expand()
    }
    trainRoboUnits()
    trainGatewayUnits()
    once(3, Protoss.Dragoon)
    expand()
    get(Protoss.RoboticsFacility, Protoss.Observatory, Protoss.Observer, Protoss.RoboticsSupportBay)
    get(2, Protoss.Gateway)
  }

  private[PvP] def runGatewayTech(): Unit = {
    get(Protoss.DragoonRange)
    if (With.scouting.ourProximity < 0.5 && safePushing && ! With.fingerprints.robo() && With.sense.netDamage > 400) {
      get(Protoss.RoboticsFacility, Protoss.Observatory, Protoss.Observer)
    }
    if (enemyDarkTemplarLikely) {
      get(Protoss.RoboticsFacility, Protoss.Observatory, Protoss.Observer)
    }
    if (shouldExpand) {
      pumpWorkers(oversaturate = true)
      if (PvP4GateGoon() && enemyBases < 2) {
        buildCannonsAtFoyer(1)
      }
      expand()
    }
    once(2, Protoss.Dragoon)
    get(?(PvP3GateGoon(), 3, 4), Protoss.Gateway)
    if (safePushing && unitsComplete(Protoss.Gateway) >= 3 && unitsEver(Protoss.Dragoon) >= 10) {
      buildCannonsAtFoyer(1)
    }
    trainGatewayUnits()
    if (PvP3GateGoon()) { expand() }
  }

  private[PvP] def finishTech(): Unit = {
    get(Protoss.DragoonRange)
    pumpWorkers(oversaturate = true)
    get(4, Protoss.Gateway)
    expand()
  }

  private def trainRoboUnits(): Unit = {
    var pumpObs = getObservers
    pumpObs &&= ! With.fingerprints.fourGateGoon()
    pumpObs &&= ! enemyRobo
    pumpObs &&= enemyBases < 2
    pumpObs &&= ! reaverAllIn
    pumpObs &&= units(Protoss.Reaver) >= 2 * units(Protoss.Shuttle)
    pumpObs ||= With.fingerprints.dtRush()
    if (getObservers) once(Protoss.Observer)
    if (pumpObs) pump(Protoss.Observer, 2) else if (units(Protoss.Observer) > 1) cancel(Protoss.Observer)
    if (reaverAllIn || units(Protoss.Reaver) >= 2) pumpShuttleAndReavers() else pump(Protoss.Reaver)
  }

  private def trainGatewayUnits(): Unit = {
    if (zAfterCore && zBeforeCore) once(2, Protoss.Zealot)
    else if (zAfterCore || zBeforeCore) once(Protoss.Zealot)
    once(Protoss.Dragoon)
    pump(Protoss.Dragoon)
    pumpGasPumps()
    if ( ! PvPCoreExpand() || gas < 32) {
      pump(Protoss.Zealot)
    }
  }

  private def expand(): Unit = {
    if ( ! With.scouting.enemyControls(With.geography.ourFoyer)) {
      requireMiningBases(2)
    }
  }
}
