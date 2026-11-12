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
      get(Protoss.RoboticsFacility, Protoss.Observatory, Protoss.Observer)
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
      get(Protoss.Forge)
      get(Protoss.GroundDamage)
      get(Protoss.CitadelOfAdun)
      get(Protoss.ZealotSpeed)
      get(Protoss.TemplarArchives)

      if ( ! anticipateSpeedlings && ! enemyHydralisksLikely) {
        get(Protoss.Stargate)
        get(Protoss.Corsair)
      }

      if (have(Protoss.Corsair) || (anticipateSpeedlings && ! safeDefending)) {
        if (DarkTemplarEffectiveness.allows(
          UnitArchive.producedTotal(Protoss.DarkTemplar),
          units(Protoss.DarkTemplar),
          gas)) {
          get(Protoss.DarkTemplar)
        }
      }
    }

    if (on(Goon)) {
      get(Protoss.CyberneticsCore)
      get(Protoss.DragoonRange)
    }

    if (on(Reaver)) {
      get(Protoss.RoboticsFacility)
      get(Protoss.RoboticsSupportBay)
    }

    if (on(Stargate)) {
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
    get(Protoss.Forge)
    get(Protoss.GroundDamage)
    if (makeCorsairs) {
      get(2, Protoss.Corsair)
    }
    get(Protoss.CitadelOfAdun)
    get(Protoss.ZealotSpeed)
    if (enemyMutalisksLikely) {
      buildCannonsAtMain(2, PlaceLabels.DefendAir)
    }
    get(Protoss.TemplarArchives)
    techSpeedlotCasters()
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
   * Dark Templars first (four lifetime, and only once Corsairs or speedlings
   * let them in), then High Templars at 150 supply - or sooner if Mutalisks
   * force faster Archons (mirror of ZealotIntoGoon).
   */
  private def techSpeedlotCasters(): Unit = {
    val castersWelcome = have(Protoss.Corsair) || (anticipateSpeedlings && ! safeDefending)
    if (castersWelcome && DarkTemplarEffectiveness.allows(
      UnitArchive.producedTotal(Protoss.DarkTemplar),
      units(Protoss.DarkTemplar),
      gas)) {
      get(Protoss.DarkTemplar)
    }
    if (supplyUsed200 >= 150 && (UnitArchive.producedTotal(Protoss.DarkTemplar) >= 4 || enemies(Zerg.Mutalisk) >= 5)) {
      get(2, Protoss.HighTemplar)
    }
  }

  private def techReaver(): Unit = {
    cancelAlternative(Protoss.Stargate, Protoss.CitadelOfAdun, Protoss.Forge)
    cancelRangeUnlessGoons()
    get(Protoss.RoboticsFacility)
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
    get(Protoss.Stargate)
    if (enemyMutalisksLikely || ! enemyHydralisksLikely) {
      get(Protoss.Corsair)
    }
    if (enemyMutalisksLikely) {
      get(Protoss.DragoonRange)
    } else {
      get(Protoss.CitadelOfAdun)
      get(Protoss.ZealotSpeed)
    }
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
