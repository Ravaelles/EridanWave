package Micro.Actions.Combat.Decisioning

import Debugging.Visualizations.{ForceLabel, Forces}
import Mathematics.Physics.Force
import Micro.Actions.Combat.Decisioning.Techniques._
import Micro.Actions.Protoss.Reaver.BeReaver
import Micro.Actions.Combat.Decisioning.Formation.{FormationHold, OverwhelminglyStronger}
import Micro.Agency.Commander
import Micro.Coordination.Pathing.MicroPathing
import Micro.Coordination.Pushing.TrafficPriorities
import Micro.Heuristics.Potential
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.?

class CombatManeuvers(combat: Combat, unit: FriendlyUnitInfo) {

  private[Decisioning] def applyForce(label: ForceLabel, force: Force): Unit = {
    if (force.lengthFast > 0) {
      unit.agent.forces.put(label, force)
    }
  }

  private[Decisioning] def applySeparationForces(): Unit = {
    applyForce(Forces.spacing,  Potential.preferSpacing(unit))
    applyForce(Forces.pushing,  Potential.followPushes(unit))
  }

  private[Decisioning] def moveForcefully(): Boolean = {
    applySeparationForces()
    val radians   = unit.agent.forces.sum.radians
    val forceGoal = MicroPathing.getWaypointInDirection(unit, radians).orElse(unit.agent.destinationNext.pixel)
    unit.agent.forced.set(forceGoal)
    move()
  }

  private[Decisioning] def move(): Boolean = {
    if (unit.canMove) {
      if (Protoss.Reaver(unit) && unit.agent.ride.isDefined && ! unit.agent.commit && ! unit.loaded) {
        if (combat.target
          .map(t => Math.min(unit.pixelsToGetInRange(t), unit.pixelsToGetInRange(t, t.projectFrames(48))) > 64)
          .getOrElse(unit.pixelDistanceTravelling(unit.agent.destinationNext()) > 96)) {
          BeReaver.clickShuttle(unit)
        }
      }
      Commander.move(unit)
    }
    unit.unready
  }

  private[Decisioning] def walk(): Boolean = {
    unit.agent.shouldFight = true // This encourages the unit to use its forward formation rather than its retreat formation
    unit.agent.fightReason = "Walking"

    unit.agent.perch.clear() // We're not picking a fight just yet
    if (unit.intent.canSneak) {
      MicroPathing.tryMovingAlongTilePath(unit, MicroPathing.getSneakyPath(unit))
    } else if (unit.agent.receivedPushPriority() > unit.agent.priority) {
      applyForce(Forces.travel, Potential.towards(unit, unit.agent.destinationNext()))
      moveForcefully()
    } else {
      move()
    }
    unit.unready
  }

  private[Decisioning] def tryDodge(): Boolean = {
    if ( ! unit.ready || ! unit.canMove || unit.loaded || unit.agent.receivedPushPriority() < TrafficPriorities.Dodge) return false

    combat.technique = Dodge
    unit.agent.act(combat.technique.toString)
    dodge()
  }

  private[Decisioning] def dodge(): Boolean = {
    if (unit.ready) {
      unit.agent.shouldFight = false // Emergency movement must not become an attack-move when sluggish.
      unit.agent.fightReason = "Dodging"
      unit.agent.escalatePriority(TrafficPriorities.Shove) // Just lower than the dodge itself
      unit.agent.forces(Forces.threat) = Potential.followPushes(unit)
      MicroPathing.moveForcefully(unit)
    }
    unit.unready
  }

  private[Decisioning] def chase(): Boolean = {
    if (unit.ready && combat.target.isDefined) {
      unit.agent.act("Chase")
      val to                  = combat.target.get.pixel
      val step                = combat.target.get.presumptiveDestinationNext
      val chaseGoal           = ?(step.traversableBy(unit) && unit.pixelDistanceSquared(step) >= unit.pixelDistanceSquared(to), step, to)
      val extraChaseDistance  = Math.max(0, unit.pixelDistanceCenter(chaseGoal) - unit.pixelDistanceCenter(to))
      unit.agent.decision.set(unit.pixel.project(chaseGoal, combat.idealDistanceForward + extraChaseDistance))
      unit.agent.escalatePriority(TrafficPriorities.Nudge)
      move()
    }
    unit.unready
  }

  private[Decisioning] def charge(): Boolean = {
    if (unit.ready && ( ! combat.target.exists(unit.inRangeToAttack) || ! AttackIfReadyBehavior.run(unit, combat))) {
      // If we have an attack formation
      if ( ! OverwhelminglyStronger.skipFormation(unit) && unit.formationEngage.isDefined && ! unit.flying && combat.shouldChase && combat.formationHelpsChase && FormationHold.shouldAdvanceInFormation(combat.formationReadiness)) {
        unit.agent.act("Slide")
        unit.agent.escalatePriority(TrafficPriorities.Nudge)
        move()

      } else if (unit.isAny(Terran.Wraith, Protoss.Corsair, Protoss.Scout, Zerg.Mutalisk, Zerg.Scourge) && combat.target.exists(t =>
        t.flying
        && unit.pixelDistanceEdge(t)          > 0.25 * unit.pixelRangeAgainst(t)
        && unit.speedApproachingEachOther(t)  < 0
        && unit.speedApproaching(t)           < 0.9 * unit.topSpeed)) {
        unit.agent.act("Dogfight")
        chase()

      } else if (combat.breakFormationToAttack) {
        if (combat.shouldChase && combat.idealDistanceForward >= 32) {
          chase()
        } else {
          unit.agent.act("Approach")
          Commander.attack(unit)
        }
      } else {
        unit.agent.act("Move")
        move()
      }
    }
    unit.unready
  }
}
