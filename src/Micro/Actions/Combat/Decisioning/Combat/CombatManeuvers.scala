package Micro.Actions.Combat.Decisioning

import Debugging.Visualizations.{ForceLabel, Forces}
import Lifecycle.With
import Mathematics.Maff
import Mathematics.Physics.Force
import Micro.Actions.Combat.Decisioning.Techniques._
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
    unit.agent.forceTo(forceGoal)
    move()
  }

  private[Decisioning] def move(): Boolean = {
    if (unit.canMove) {
      if (Protoss.Reaver(unit) && unit.agent.ride.isDefined && ! unit.agent.commit && ! unit.loaded) {
        if (combat.target
          .map(t => Math.min(unit.pixelsToGetInRange(t), unit.pixelsToGetInRange(t, t.projectFrames(48))) > 64)
          .getOrElse(unit.pixelDistanceTravelling(unit.agent.destinationNext()) > 96)) {
          Commander.rightClick(unit, unit.agent.ride.get)
        }
      }
      Commander.move(unit)
    }
    unit.unready
  }

  private[Decisioning] def walk(): Boolean = {
    // No vote here: the skirmish verdict already spoke (FightOrFlee runs
    // first), and safe passage inherits it - usually willingness to trot,
    // but a unit walking home from contact keeps its disengage formation
    // instead of being re-armed by the march it just left.
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

    // Same gate as the technique transition above: a suppressed label
    // with a live dodge (or the reverse) reads as flapping, not doctrine.
    if ( ! Micro.Actions.Combat.Maneuvering.DOdge.shouldDodge(
      Information.Geography.BaseLocations.main.map(base => unit.pixelDistanceCenter(base.heart.center) * Maff.inv32).getOrElse(Double.PositiveInfinity),
      Information.Geography.Distances.naturalChokeToMainBase(),
      unit.cooldownLeft)) return false

    combat.technique = Dodge
    unit.agent.act(combat.technique.toString)
    dodge()
  }

  private[Decisioning] def dodge(): Boolean = {
    if (unit.ready) {
      unit.agent.decideFight(false) // Emergency movement must not become an attack-move when sluggish.
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
      // Loaded volley owns the frame: walking past a hittable shot to
      // close distance the hull already holds donates the volley for
      // positioning. Hold and spend it instead - the charge resumes once
      // the gun cools. Air dancers keep their dance (motion is their
      // survival); everyone else stands and shoots. One verdict, shared.
      if (VolleyDiscipline.shouldVolley(retreating = false, unit.cooldownLeft, unit.matchups.targetsInRange.nonEmpty) && ! dogfightingAir) {
        unit.agent.act("VolleyHold")
        Commander.hold(unit)
      }
      else if (slidingInFormation) slideInFormation()
      else if (dogfightingAir) dogfightAir()
      else if (combat.breakFormationToAttack) breakToAttack()
      else moveToSlot()
    }
    unit.unready
  }

  private def slidingInFormation: Boolean =
    // If we have an attack formation
    ! OverwhelminglyStronger.skipFormation(unit) && unit.formationEngage.isDefined && ! unit.flying && combat.shouldChase && combat.formationHelpsChase && FormationHold.shouldAdvanceInFormation(combat.formationReadiness)

  private def dogfightingAir: Boolean =
    unit.isAny(Terran.Wraith, Protoss.Corsair, Protoss.Scout, Zerg.Mutalisk, Zerg.Scourge) && combat.target.exists(t =>
      t.flying
      && unit.pixelDistanceEdge(t)          > 0.25 * unit.pixelRangeAgainst(t)
      && unit.speedApproachingEachOther(t)  < 0
      && unit.speedApproaching(t)           < 0.9 * unit.topSpeed)

  private def slideInFormation(): Unit = {
    unit.agent.act("Slide")
    unit.agent.escalatePriority(TrafficPriorities.Nudge)
    move()
  }

  private def dogfightAir(): Unit = {
    unit.agent.act("Dogfight")
    chase()
  }

  private def breakToAttack(): Unit = {
    if (combat.shouldChase && combat.idealDistanceForward >= 32) {
      chase()
    } else if (regroupWithPack()) {
      // Acted and moved inside: the pack recenters instead of charging.
    } else {
      unit.agent.act("Approach")
      Commander.attack(unit)
    }
  }

  /**
   * Zealot pack regroup (unit-owned claim): a lone hull of a small
   * zealots-only force returns to the centroid instead of approaching
   * alone - unless the local skim runs two-to-one or better. Feeding
   * piecemeal loses the ball fight before it starts.
   */
  private def regroupWithPack(): Boolean = {
    if ( ! Protoss.Zealot(unit)) return false

    // Already fighting hulls never regroup out: the rule stops lone
    // charges into the fight, and dragging fighters back out reverses
    // it - the duel owns the frame while the gun or the enemy is hot.
    val inContact = unit.matchups.targetsInRange.nonEmpty || unit.matchups.engagedUpon
    // Closing enemies are contact too: a mutual approach is a joined
    // fight, and regrouping home through it feeds the pack piecemeal.
    // A camping enemy still lets the lone hull walk home.
    val enemyClosing = unit.matchups.threats.exists(t =>
      t.unitClass.isWarrior && (t.presumptiveTarget.contains(unit) || t.speedApproaching(unit) > 0))
    if (inContact) return false

    val warriors = With.units.ours.view.filter(u => u.alive && u.unitClass.isWarrior).take(ZealotPack.packSize + 1).toSeq
    if (warriors.size >= ZealotPack.packSize || ! warriors.forall(Protoss.Zealot(_))) return false

    // Anchor on the fighting pack, not the squad: a whole-squad centroid
    // sits wherever the uncommitted mass is and drags the duel home
    // instead of rejoining it. Warriors only - workers do not anchor blades.
    // No fighting mates means no fight to join yet: fall back to the whole
    // pack so lone hulls still group up before contact.
    val mates = unit.squad.map(_.units.filter(u =>
      u.id != unit.id && u.alive && u.unitClass.isWarrior)).getOrElse(Seq.empty)
    val fighting = mates.filter(m =>
      m.matchups.targetsInRange.nonEmpty || m.matchups.engagedUpon)
    val anchor = if (fighting.nonEmpty) fighting else mates
    val centroid = if (anchor.isEmpty) unit.pixel else Maff.centroid(anchor.map(_.pixel))
    val mateTiles = Maff.minBy(With.units.ours.view.filter(o =>
      o.id != unit.id && o.alive && Protoss.Zealot(o)))(_.pixelDistanceEdge(unit.pixel))
      .map(_.pixelDistanceEdge(unit.pixel) * Maff.inv32)
      .getOrElse(Double.PositiveInfinity)
    val (usSkim, foeSkim) = unit.combatForecast
      .map(battle => (battle.us.skimStrengthTotal, battle.enemy.skimStrengthTotal))
      .getOrElse((0.0, 0.0))
    if ( ! ZealotPack.shouldRegroup(onlyZealots = true, warriors.size, mateTiles, usSkim, foeSkim, inContact, enemyClosing)) return false

    unit.agent.act("PackRegroup")
    unit.agent.forceTo(centroid)
    Commander.move(unit)

    true
  }

  private def moveToSlot(): Unit = {
    unit.agent.act("Move")
    move()
  }
}
