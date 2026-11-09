package Gameplans.Eridan.PvZ

import Lifecycle.With
import Macro.Actions.{Compositor, MacroActions}
import Macro.Facts.UnitArchive
import ProxyBwapi.Races.{Protoss, Zerg}
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
    compositor.setNeed(Protoss.Zealot, 4.0)
    compositor.setGoal(Protoss.Zealot, zealotTarget)
    if (units(Protoss.Zealot) >= zealotTarget && have(Protoss.CyberneticsCore)) {
      compositor.setNeed(Protoss.Dragoon, 2.0)
      compositor.setGoal(Protoss.Dragoon, 6 + enemies(Zerg.Mutalisk) + enemies(Zerg.Hydralisk))
    }
    if (supplyUsed200 >= 85) {
      produceAdvanced()
    }
    compositor.produceAndPump()
    status(f"ZealotIntoGoon: want $zealotTarget zealots vs ${enemies(Zerg.Zergling)} lings")
    directPosture()
    ensureLurkerDetection()
    expandWhenAhead()
  }

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
    if (have(Protoss.RoboticsSupportBay)) {
      compositor.setNeed(Protoss.Reaver, 3.0)
      compositor.setGoal(Protoss.Reaver, 6)
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
      pump(Protoss.DarkTemplar, 4)
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
