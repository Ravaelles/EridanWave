package Gameplans.Protoss.PvZ.reactive

import Lifecycle.With
import Macro.Facts.UnitArchive
import Placement.Access.PlaceLabels
import ProxyBwapi.Buildable
import ProxyBwapi.Races.{Protoss, Zerg}
import Tactic.Missions.evaluation.DarkTemplarEffectiveness
import Utilities.SwapIf
import Utilities.UnitFilters.{IsWarrior, IsWorker}

/**
 * Tech plan: which structures the chosen composition needs, which competing
 * structures to cancel, and the gas/detection floors that keep the plan fed.
 */
trait PvZReactiveTech extends PvZ1BaseReactiveUtilities {

  protected def composition: Seq[Composition]
  protected def on(c: Composition*): Boolean

  protected def capGas(): Unit = {
    if (have(Protoss.TemplarArchives)) return

    if (anticipateSpeedlings && units(IsWarrior) < 9 && units(Protoss.Gateway) < 3) {
      gasWorkerCeiling(0)
    } else if (composition.length == 1 && ! on(Speedlot)) {
      gasLimitCeiling(300)
    }
  }
  protected def detectionVsLurker(): Unit = {
    if (enemyHasShown(Zerg.Lurker, Zerg.LurkerEgg) || With.blackboard.expoBuildStalled()) {
      status("Lurkers")
      if ( ! prioritizeTemplarsOverFirstRobo()) {
        get(Protoss.RoboticsFacility)
      }
      get(Protoss.Observatory, Protoss.Observer)
      if (enemyHasShown(Zerg.Lurker, Zerg.LurkerEgg)) {
        if (enemies(Zerg.Lurker) > 0 && ! have(Protoss.Observer)) {
          status("LurkerDanger")
          buildCannonsAtOpenings(1)
        }
        pump(Protoss.Observer, 2)
      } else {
        pump(Protoss.Observer)
      }
    }
  }

  /**
   * Tech floor: past the SwapIf supply threshold only buildArmy runs, so
   * without this the composition tech starves - fifty Zealots with no
   * Assimilator and no Citadel lose to anything with legs. Idempotent
   * gets, all no-ops once satisfied; early game stays on the openings.
   */
  protected def ensureCompositionTech(): Unit = {
    // Gas floor above composition: a standing army with no gas path
    // never techs, whatever the scouting says - order it here so no
    // opening, proxy or empty composition can starve it.
    if (units(IsWarrior) >= 10) {
      get(Protoss.Assimilator)
      get(Protoss.CyberneticsCore)
    }

    if (composition.isEmpty) return

    get(Protoss.Assimilator)
    if (units(Protoss.Gateway) >= 5 || units(IsWorker) > 35) {
      pumpGasPumps()
    }

    if (on(Speedlot)) {
      if ( ! anticipateSpeedlings && ! enemyHydralisksLikely) {
        get(Protoss.Stargate)
        get(Protoss.Corsair)
      }
      speedlotCore()
    }

    if (on(Goon)) {
      get(Protoss.CyberneticsCore)
      get(Protoss.DragoonRange)
    }

    // Stays separate from techReaver: the scheduled path swaps the Bay
    // for Citadel/Speed/Shuttle while unsafe - the floor always covers it.
    if (on(Reaver)) {
      get(Protoss.RoboticsFacility)
      get(Protoss.RoboticsSupportBay)
    }

    if (on(Stargate)) {
      stargateCore()
    }
  }

  /**
   * Shared Speedlot core (no cancels, no counts): the floor and the
   * scheduled tech order the same structures, so they read one list.
   * Counts (Corsairs), cancels, cannons and High Templars stay with
   * buildTech - the floor must stay lean while the army builds.
   */
  private def speedlotCore(): Unit = {
    get(Protoss.Forge)
    get(Protoss.GroundDamage)
    get(Protoss.CitadelOfAdun)
    get(Protoss.ZealotSpeed)
    get(Protoss.TemplarArchives)
    if ((have(Protoss.Corsair) || (anticipateSpeedlings && ! safeDefending)) && DarkTemplarEffectiveness.allows(
      UnitArchive.producedTotal(Protoss.DarkTemplar),
      units(Protoss.DarkTemplar),
      gas)) {
      get(Protoss.DarkTemplar)
    }
  }

  /**
   * Shared Stargate core: the floor and the scheduled tech order the same
   * structures (the floor also covers Cybernetics, which buildTech orders
   * at its head - the duplicate get is a no-op).
   */
  private def stargateCore(): Unit = {
    get(Protoss.Stargate)
    if (enemyMutalisksLikely || ! enemyHydralisksLikely) {
      get(Protoss.Corsair)
    }
    if (enemyMutalisksLikely) {
      get(Protoss.CyberneticsCore)
      get(Protoss.DragoonRange)
    } else {
      get(Protoss.CitadelOfAdun)
      get(Protoss.ZealotSpeed)
    }
  }

  def buildTech(): Unit = {
    get(Protoss.Gateway, Protoss.Assimilator, Protoss.CyberneticsCore)
    if (units(Protoss.Gateway) >= 5 || units(IsWorker) > 35) {
      pumpGasPumps()
    }

    composition.foreach {
      case Goon     => techGoon()
      case Speedlot => techSpeedlot()
      case Reaver   => techReaver()
      case Stargate => techStargate()
    }
  }

  private def techGoon(): Unit = {
    cancelRoboUnlessLurker()
    cancelAlternative(Protoss.CitadelOfAdun, Protoss.Forge)
    get(Protoss.DragoonRange)
  }

  private def techSpeedlot(): Unit = {
    cancelRoboUnlessLurker()
    cancelRangeUnlessGoons()
    val makeCorsairs = ! anticipateSpeedlings && ! enemyHydralisksLikely
    techSpeedlotCorsairs(makeCorsairs)
    if (makeCorsairs) {
      get(2, Protoss.Corsair)
    }
    if (enemyMutalisksLikely) {
      buildCannonsAtMain(2, PlaceLabels.DefendAir)
    }
    speedlotCore()
    techSpeedlotHighTemplars()
  }

  /** Corsairs are the Speedlot answer to Mutalisks, and a liability otherwise. */
  private def techSpeedlotCorsairs(makeCorsairs: Boolean): Unit = {
    if (makeCorsairs) {
      get(Protoss.Stargate)
      get(Protoss.Corsair)
    } else {
      cancelAlternative(Protoss.Stargate)
    }
  }

  /**
   * High Templars at 150 supply (four lifetime Dark Templars first) - or
   * sooner if Mutalisks force faster Archons (mirror of ZealotIntoGoon).
   * The Dark Templar half of the old casters gate now lives in
   * speedlotCore, shared with the floor.
   */
  private def techSpeedlotHighTemplars(): Unit = {
    if (supplyUsed200 >= 150 && (UnitArchive.producedTotal(Protoss.DarkTemplar) >= 4 || enemies(Zerg.Mutalisk) >= 5)) {
      get(2, Protoss.HighTemplar)
    }
  }

  /**
   * First Robo waits for two lifetime Dark Templars while a complete
   * Archives stands unused, the bank is thin, and no Lurkers are shown
   * or likely - the sunk Archives earns before the next tech path opens.
   * Queues the Dark Templar when it fires so the release condition moves;
   * the scheduler still decides affordability as usual.
   */
  protected def prioritizeTemplarsOverFirstRobo(): Boolean = {
    val prioritize = Gameplans.Protoss.PvZ.policy.TemplarArchivesOrRoboticsFacility.shouldPrioritizeTemplarsOverFirstRoboticsFacility(
      haveArchives = haveComplete(Protoss.TemplarArchives),
      hasRobo      = have(Protoss.RoboticsFacility),
      dtProduced   = UnitArchive.producedTotal(Protoss.DarkTemplar),
      gas          = gas,
      lurkersShown = enemyHasShown(Zerg.Lurker, Zerg.LurkerEgg),
      lurkersLikely = enemyLurkersLikely)
    if (prioritize) {
      get(Protoss.DarkTemplar)
      status("TemplarsBeforeRobo")
    }
    prioritize
  }

  private def techReaver(): Unit = {
    cancelAlternative(Protoss.Stargate, Protoss.CitadelOfAdun, Protoss.Forge)
    cancelRangeUnlessGoons()
    if ( ! prioritizeTemplarsOverFirstRobo()) {
      get(Protoss.RoboticsFacility)
    }
    SwapIf(
      safeDefending,
      {
        get(Protoss.RoboticsSupportBay)
        get(2, Protoss.Reaver)
      },
      {
        get(Protoss.CitadelOfAdun)
        if (safeDefending || units(Protoss.Reaver) >= 2) {
          get(Protoss.Shuttle)
        }
        get(Protoss.ZealotSpeed)
      })
  }

  private def techStargate(): Unit = {
    cancelRoboUnlessLurker()
    cancelRangeUnlessGoons()
    cancelAlternative(Protoss.Forge)
    stargateCore()
  }

  private def cancelAlternative(buildables: Buildable*): Unit = {
    if (composition.length <= 1) {
      cancel(buildables: _*)
    }
  }

  private def cancelRoboUnlessLurker(): Unit = {
    if (composition.length <= 1 && ! enemyLurkersLikely) {
      cancel(Protoss.RoboticsFacility)
    }
  }

  private def cancelRangeUnlessGoons(): Unit = {
    if (composition.length <= 1 && units(Protoss.Dragoon) < 3) {
      cancel(Protoss.DragoonRange)
    }
  }

}
