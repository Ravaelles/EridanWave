package Gameplans.Eridan.PvZ

import Lifecycle.With
import Macro.Actions.{Compositor, MacroActions}
import Macro.Facts.UnitArchive
import ProxyBwapi.Races.{Protoss, Zerg}
import Tactic.Missions.evaluation.DarkTemplarEffectiveness
import Utilities.?
import Utilities.UnitFilters.IsWarrior

/**
 * Gateway production policy: hold early Zergling pressure with Zealots,
 * then switch to Dragoons once the Zealot wall stands.
 *
 * Rule: always want 2 Zealots, plus one more per 4 scouted (visible) enemy
 * Zerglings, up to 6 Zealots; Dragoons only after the Zealot target is met
 * and the Cybernetics Core exists (Pump drops requests whose tech is
 * missing anyway, so an early Dragoon need is harmless).
 *
 * Not a Modal mode and not an Opening queue: call produce() from a
 * gameplan's buildArmy() and return early, replacing its own unit mix.
 * Selected by default in every PvZ1BaseReactive branch (see its addChoice);
 * see PvZ1BaseReactive.buildArmy for the wiring.
 */
object ZealotIntoGoon extends MacroActions {

  def zealotTarget: Int = Math.min(6, 2 + enemies(Zerg.Zergling) / 4)

  private val compositor = new Compositor

  def produce(): Unit = {
    compositor.reset()
    queueZealots()
    queueDragoons()
    if (supplyUsed200 >= 85) {
      produceAdvanced()
    }
    compositor.produceAndPump()
    pumpBankedZealotsWhenAllowed()
    status(f"ZealotIntoGoon: want $zealotTarget zealots vs ${enemies(Zerg.Zergling)} lings")

    directPosture()
    ensureLurkerDetection()
    expandWhenAhead()
  }

  // The line first: Zealots until the count fits the Ling pack.
  // Dragoons always outrank Zealots (see queueDragoons), so the line
  // takes the smaller need; past 190 supply it stands down entirely.
  private def queueZealots(): Unit = {
    if (supplyCapped) return

    compositor.setNeed(Protoss.Zealot, 2.0)
    compositor.setGoal(Protoss.Zealot, zealotTarget)
  }

  // Dragoons once the line stands: Zealots at count, Core up, bank allows.
  // The top need, so production fills Dragoons before more Zealots.
  private def queueDragoons(): Unit = {
    if ( ! zealotLineStands) return
    if ( ! have(Protoss.CyberneticsCore)) return
    if ( ! Gameplans.Protoss.PvZ.GasBank.allowsDragoon(units(Protoss.Dragoon), gas, With.units.ours.count(_.unitClass.isWarrior))) return

    compositor.setNeed(Protoss.Dragoon, 4.0)
    compositor.setGoal(Protoss.Dragoon, 6 + enemies(Zerg.Mutalisk) + enemies(Zerg.Hydralisk))
  }

  private def supplyCapped: Boolean =
    supplyUsed200 >= ZealotBank.maxSupply

  private def zealotLineStands: Boolean =
    units(Protoss.Zealot) >= zealotTarget

  // Banked minerals plus idle Gates means the line is starving, not the
  // bank: past the bank with two free Gates, queue Zealots uncapped.
  private def pumpBankedZealotsWhenAllowed(): Unit = {
    if (supplyCapped) return
    if ( ! ZealotBank.allowsExtraZealots(minerals, freeGateways)) return

    pump(Protoss.Zealot)
  }

  private def freeGateways: Int =
    With.units.ours.count(u => Protoss.Gateway(u) && u.complete && u.trainingQueue.isEmpty)

  private def expandWhenAhead(): Unit = {
    // Second base at 75 supply, but only from a position of strength
    // (Atlantis-style: 110 = 10% stronger, with a margin against noise).
    // requireMiningBases is a no-op when two mining bases already exist.
    if (supplyUsed200 >= 75 && armyStrength >= 110.0) {
      requireMiningBases(2)
      status(f"ZealotIntoGoonExpand at strength ${armyStrength.toInt}")
    }
  }

  private def produceAdvanced(): Unit = {
    // Late-game mix, mirroring PvZ1BaseReactive.buildArmy: pump whatever
    // advanced tech the composition opened, so production keeps scaling
    // after the Zealot/Dragoon core is up. The tech buildings themselves
    // still follow composition (scouting decides Robo vs Archives vs
    // Stargate); without the tech these needs stay unfulfilled.
    if (have(Protoss.RoboticsSupportBay) && Gameplans.Protoss.PvZ.ReaverGas.allowsReaver(units(Protoss.Reaver), gas)) {
      compositor.setNeed(Protoss.Reaver, 3.0)
      compositor.setGoal(Protoss.Reaver, 6)
    }
    // A Shuttle for the ferry always queues; one only for Reavers waits on gas.
    if (have(Protoss.RoboticsSupportBay) && (With.blackboard.ferry() || Gameplans.Protoss.PvZ.ReaverGas.allowsReaver(units(Protoss.Reaver), gas))) {
      compositor.setNeed(Protoss.Shuttle, 3.0)
      compositor.capGoal(Protoss.Shuttle, units(Protoss.Reaver) / 2)
    }
    if (have(Protoss.TemplarArchives)) {
      // High Templars only at 150+ supply, and four Dark Templars before
      // them (lifetime count), unless Mutalisks force faster Archons.
      if (supplyUsed200 >= 150 && (UnitArchive.producedTotal(Protoss.DarkTemplar) >= 4 || enemies(Zerg.Mutalisk) >= 5)) {
        compositor.setNeed(Protoss.HighTemplar, 2.0)
        compositor.setGoal(Protoss.HighTemplar, 4)
        pump(Protoss.HighTemplar)
      }
      if (DarkTemplarEffectiveness.allows(
        UnitArchive.producedTotal(Protoss.DarkTemplar),
        units(Protoss.DarkTemplar),
        gas)) {
        pump(Protoss.DarkTemplar, 4)
      }
    }
    val airThreat = enemies(Zerg.Mutalisk, Zerg.Scourge)
    if (have(Protoss.Stargate) && airThreat > 0) {
      compositor.setNeed(Protoss.Corsair, 3.0)
      compositor.setGoal(Protoss.Corsair, 2 * airThreat)
    }
    if ( ! enemyHydralisksLikely) {
      pump(Protoss.Scout)
    }
  }

  private def directPosture(): Unit = {
    // Constant early pressure; pull back only on evidence we'd lose.
    // attack() only votes yes, so set the blackboard directly: produce()
    // runs after the gameplan's own attack vote and the last write wins.
    val tooWeakToFight = unitsComplete(IsWarrior) < 2 || confidenceAttacking11 < 0
    With.blackboard.wantToAttack.set(! tooWeakToFight)
    With.blackboard.aggressionRatio.set(?(tooWeakToFight, 0.75, 1.5))
    status(tooWeakToFight, "ZealotIntoGoonRetreat")
  }

  private def ensureLurkerDetection(): Unit = {
    // Mirrors detectionVsLurker (which also runs): duplicate get()/pump()
    // requests are harmless - they all say "want N total", never "build N more".
    if (enemyHasShown(Zerg.Lurker, Zerg.LurkerEgg)) {
      get(Protoss.RoboticsFacility, Protoss.Observatory)
      pump(Protoss.Observer, 2)
      status("ZealotIntoGoonVsLurker")
    }
  }
}
