package Tactic.Squads

import Lifecycle.With
import Mathematics.Points.Pixel
import Micro.Formation.{Formation, Formations}
import Tactic.Squads.Decisions.{AttackModes, SquadMarch}
import Tactic.Squads.attack.SquadAttack

import scala.collection.mutable.ArrayBuffer

/**
 * Squad formations (extracted from SquadAutomation): which arcs the squad
 * stands in. One reason to change: formation choice per march state.
 * SquadAutomation keeps a thin forwarder so call sites do not move.
 */
object SquadForming {

  def form(squad: Squad, from: Pixel, to: Pixel): ArrayBuffer[Formation] = {
    val output: ArrayBuffer[Formation] = ArrayBuffer.empty
    // If advancing, give a formation for forward movement.
    // March as soon as enemies are known en route (Stardust: form the line
    // before contact), not only after the squad voted to fight.
    if (SquadMarch.shouldFormMarch(squad.fightConsensus, squad.battleEnemies.nonEmpty)) {
      // Contain dressing rides the march formation: melee drops back so the
      // Dragoons keep the inner arc and shoot.
      val meleeBack = squad match {
        case attack: SquadAttack => attack.mode == AttackModes.ContainArmy
        case _                   => false
      }
      output += Formations.march(squad, to, meleeBack)
    }
    // Always include a disengagey formation for units that want to retreat/kite
    if (squad.centroidKey.zone == squad.homeConsensus.zone && With.scouting.enemyThreatOrigin.zone != squad.homeConsensus.zone) {
      output += Formations.guard(squad, Some(squad.homeConsensus))
    } else {
      output += Formations.disengage(squad)
    }
    output
  }
}
