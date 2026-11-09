package Micro.Actions.Combat.Decisioning

import Debugging.Visualizations.Forces
import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.Techniques._
import Micro.Agency.Commander
import Micro.Coordination.Pathing.MicroPathing
import Micro.Coordination.Pushing.TrafficPriorities
import Micro.Heuristics.Potential
import Micro.Targeting.FiltersSituational.{TargetFilterPotshot, TargetFilterVisibleInRange}
import Micro.Targeting.Target
import ProxyBwapi.Races.{Terran, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Micro.Actions.Combat.Maneuvering.Retreat
import Utilities.Time.Forever
import Utilities.UnitFilters.IsWarrior
import Utilities.?

class CombatEngagement(combat: Combat, unit: FriendlyUnitInfo) {

  private[Decisioning] def attackIfReady(): Boolean = {
    if (unit.ready && combat.target.exists(unit.readyForAttackOrder) && combat.distances.readyToApproachTarget) {
      Commander.attack(unit)
    }
    unit.unready
  }

  private[Decisioning] def aim(): Boolean = {
    if (unit.ready) {
      Target.choose(unit, TargetFilterVisibleInRange)
      Commander.attack(unit)
      Commander.hold(unit)
    }
    unit.unready
  }

  private[Decisioning] def potshot(): Boolean = {
    if (unit.ready) {
      val potshotTarget = Maff.orElseFiltered(
          Maff.minBy(unit.matchups.threats)(_.pixelDistanceEdge(unit)).filter(TargetFilterPotshot.legal(unit, _)),
          combat.target.filter(TargetFilterPotshot.legal(unit, _)),
          Target.best(unit, TargetFilterPotshot))(unit.canAttack).headOption
      if (potshotTarget.exists(t => unit.readyForAttackOrder(t) && TargetFilterPotshot.legal(unit, t))) {
        unit.agent.toAttack = potshotTarget
        unit.agent.act("Potshot")
        Commander.attack(unit)
      }
    }
    unit.unready
  }

  // Formation approach governor (FormationApproachEnemy): dress the line
  // before contact instead of trickling in piecemeal. Only while contact is
  // lost - no live enemy within 1.5 tiles of any squad unit; real contact
  // fights normally. Melee must still close, flankers still circle, yolo
  // still overrules, and without a formation there is nothing to dress.
  private[Decisioning] def approachPosture: FormationApproach.ApproachPosture =
    ?(With.enemies.exists(_.isTerran), FormationApproach.Decisive, FormationApproach.Cautious)

  // KiteAway: wounded units yield ground instead of dying in place.
  // Runs before the dress line (withdrawing beats dressing), only while an
  // enemy presses within range plus margin and the weapon still needs its
  // cooldown. The retreat point sits behind the line by injury * max offset,
  // so the most wounded absorb the least focus fire.
  private[Decisioning] def kiteAway(): Boolean = {
    if (With.yolo.active) return false
    if (unit.unitClass.melee || ! unit.canMove) return false
    if (unit.formationEngage.isEmpty) return false

    val nearest = unit.matchups.targetNearest
    if (nearest.isEmpty) return false

    val squadUnits = unit.squad.map(_.units).getOrElse(Seq(unit))
    val enemyInMargin = squadUnits.exists(_.canAnyEnemyAttackUsWithMargin(KiteAway.enemyMarginTiles))
    if ( ! KiteAway.shouldRetreat(unit.totalHealth, unit.woundPercent(), unit.cooldownLeft, enemyInMargin)) return false

    unit.agent.act("KiteAway")
    val offsetPixels = KiteAway.woundOffsetTiles(unit.woundPercent()) * 32
    unit.agent.decision.set(unit.pixel.project(nearest.get.pixel, -offsetPixels).walkablePixel)
    combat.maneuvers.move()
    true
  }

  private[Decisioning] def approach(): Boolean = {
    if (With.yolo.active) return false
    if (unit.unitClass.melee) return false
    if (unit.formationEngage.isEmpty || unit.formationEngage.exists(_.flanking)) return false

    val nearest = unit.matchups.targetNearest
    if (nearest.isEmpty) return false

    val squadUnits = unit.squad.map(_.units).getOrElse(Seq(unit))
    if (squadUnits.exists(_.canAnyEnemyAttackUsWithMargin(FightRules.contactMarginTiles))) return false

    FormationApproach.decide(
      unit.pixelsToGetInRange(nearest.get) / 32.0,
      unit.squad.flatMap(_.lineStats),
      approachPosture) match {
      case FormationApproach.FreeMarch | FormationApproach.BreakThrough => false
      case FormationApproach.HoldLine =>
        unit.agent.act("Dress")
        Commander.hold(unit)
        true
      case FormationApproach.StepForward | FormationApproach.CloseCautious =>
        unit.agent.act("Dress")
        combat.maneuvers.move()
        true
      case FormationApproach.StepBack =>
        unit.agent.act("Dress")
        if (With.frame % FormationApproach.correctionCadence == unit.id % FormationApproach.correctionCadence) {
          unit.agent.decision.set(unit.pixel.project(nearest.get.pixel, -FormationApproach.correctionStepTiles * 32))
          combat.maneuvers.move()
        } else {
          Commander.hold(unit)
        }
        true
    }
  }

  private[Decisioning] def abuse(): Boolean = {
    var minimumSpace = 16.0
    unit.matchups.threatDeepest.foreach(t => {
      if (IsWarrior(t)) {
        minimumSpace += 32
        if (t.presumptiveTarget.contains(unit)) {
          minimumSpace += 32
        }
        if (t.topSpeed > unit.topSpeed) {
          minimumSpace += 32
        }
      }
      lazy val rangeDifferential = unit.pixelRangeAgainst(t) - t.pixelRangeAgainst(unit)
      if (unit.canAttack(t) && rangeDifferential > 0) {
        minimumSpace = Math.min(minimumSpace, rangeDifferential / 2)
      }
    })
    if (combat.target.map(unit.readyForAttackOrder).getOrElse(unit.readyForAttackOrder)) {
      minimumSpace = Math.min(minimumSpace, combat.target.map(unit.pixelRangeAgainst).getOrElse(unit.pixelRangeMax) - 24)
    }

    if (unit.matchups.threatDeepest.exists(_.pixelsToGetInRange(unit) < minimumSpace)) {
      Retreat(unit)
    }
    if ( ! attackIfReady() && unit.matchups.threatDeepest.exists(a => a.presumptiveTarget.contains(unit) || a.speedApproaching(unit) > 0)) {
      Retreat(unit)
    }
    if (combat.idealDistanceForward > 0) {
      combat.maneuvers.chase()
    }
    combat.maneuvers.charge()
    unit.unready
  }

  private[Decisioning] def superkite(): Boolean = {
    combat.target.foreach(t => {
      var kite  = false
      var chase = false
      if (unit.readyForAttackOrder(t)) {
        if (unit.framesToGetInRange(t) < With.game.getRemainingLatencyFrames) {
          if (unit.orderTarget.contains(t)) {
            kite = true
          } else {
            Commander.attack(unit)
          }
        } else {
          chase = true
        }
      } else if (unit.matchups.framesOfSafety < unit.cooldownLeft + unit.cooldownMaxAgainst(t)) {
        kite = true
      }
      if (kite) {
        unit.agent.forces(Forces.threat)      = Potential.hardAvoidThreatRange(unit, CorsairSafety.avoidanceMarginPixels(unit))
        unit.agent.forces(Forces.regrouping)  = Potential.regroup(unit)
        MicroPathing.moveForcefully(unit)
      } else if (chase) {
        unit.agent.forces(Forces.target)      = Potential.towards(unit, t.pixel)
        unit.agent.forces(Forces.regrouping)  = Potential.regroup(unit) * 0.5
        MicroPathing.moveForcefully(unit)
      }
    })

    unit.unready
  }

  private[Decisioning] def engage(): Boolean = {
    if (unit.ready) {
      combat.breakFormationToAttack  = unit.formationEngage.isEmpty
      combat.breakFormationToAttack ||= combat.target.exists(targ =>
        // Maintain formation if we're not ready to attack yet
        combat.distances.readyToApproachTarget
        // Maintain formation until arriving at a fight or the destinatation
        && (unit.battle.isDefined || unit.pixelDistanceEdge(targ) <= unit.sightPixels || (unit.pixelDistanceEdge(targ) <= 256 && targ.visible))
        // Maintain formation while flanking
        && ! unit.formationEngage.exists(_.flanking)
        // Maintain formation until battle breaks out
        && ( ! combat.formationHelpsEngage || combat.squadEngagedUpon))
      combat.breakFormationToAttack ||= ! unit.formationEngage.exists(_.flanking) && combat.target.exists(unit.pixelsToGetInRange(_) < Math.max(0, 128 * unit.confidence11))
      combat.breakFormationToAttack ||= With.yolo.active

      // Hold the line while a ranged armed combat target is well beyond our
      // firing range (tiles = pixels / 32, as in Atlantis): keep formation
      // slots and let the enemy come to us instead of chasing piecemeal.
      // Buildings - even shooting ones - and unarmed units are always
      // attacked normally: there is no line to hold against them.
      // Inside ~1 tile of the range the fight starts - the unit steps in and
      // shoots. The hold also releases once the fight is on: a cluster mate
      // engaging, or an enemy in range whose weapon is cooling down (it opened
      // fire). Melee must still close, flankers must still circle, and an
      // explicit yolo overrules.
      if ( ! With.yolo.active && combat.target.exists(t =>
        FormationHold.shouldHoldLine(
          tilesBeyondRange  = unit.pixelsToGetInRange(t) / 32.0,
          melee             = unit.unitClass.melee,
          hasFormation      = unit.formationEngage.isDefined,
          flanking          = unit.formationEngage.exists(_.flanking),
          allyEngaged       = combat.squadEngagingOn,
          enemyFired        = unit.matchups.threatsInRange.exists(_.cooldownLeft > 0),
          targetHoldable    = t.unitClass.isWarrior && t.unitClass.canAttack && ! t.unitClass.isBuilding))) {
        combat.breakFormationToAttack = false
      }

      if ( ! combat.breakFormationToAttack) {
        unit.agent.perch.clear()
      }
      if (combat.pushThrough) {
        unit.agent.act("Push")
        combat.maneuvers.applySeparationForces()
        if (unit.agent.forces(Forces.spacing).lengthFast > 0 || ! attackIfReady()) {
          combat.maneuvers.applyForce(Forces.travel, Potential.towards(unit, unit.agent.station.pixel.orElse(unit.agent.perch.pixel).getOrElse(unit.agent.destinationNext())))
          combat.maneuvers.applyForce(Forces.target, Potential.towards(unit,                                 unit.agent.perch.pixel .getOrElse(unit.agent.destinationNext())))
          combat.maneuvers.applyForce(Forces.threat, Potential.softAvoidThreatRange(unit) * 1.5)
          combat.maneuvers.moveForcefully()
        }
      } else if (combat.assembling && combat.group.groupUnits.size > 1) {
        unit.agent.act("Assemble")
        val targetRangeDelta  = unit.matchups.pixelsToTargetRange.getOrElse(unit.sightPixels.toDouble) - combat.group.meanAttackerTargetDistance
        val forwardness       = if (unit.flying) 0.0 else Maff.fastTanh11(targetRangeDelta / 96.0)
        combat.maneuvers.applyForce(Forces.travel,     Potential.towards(unit, unit.agent.destinationNext()))
        combat.maneuvers.applyForce(Forces.threat,     Potential.hardAvoidThreatRange(unit, 96.0))
        combat.maneuvers.applyForce(Forces.regrouping, Potential.regroup(unit))
        combat.maneuvers.moveForcefully()
        combat.restrained = combat.target.forall(t => unit.pixelsToGetInRange(t) < unit.pixelsToGetInRangeFrom(t, unit.agent.destinationNext()))
        combat.restrained ||= unit.intent.terminus.exists(p => unit.pixelDistanceTravelling(p) < unit.pixelDistanceTravelling(unit.agent.destinationNext(), p))
      } else if (combat.techniqueIs(Scavenge)) {
        if ( ! potshot()) {
          unit.agent.act("Snipe")
          val framesSafe      = unit.matchups.threatSoonest.map(_.framesToLaunchAttack(unit)).getOrElse(Forever())
          val framesToAttack  = combat.target.map(unit.framesToLaunchAttack).getOrElse(0)
          if (framesToAttack >= framesSafe || ! attackIfReady()) {
            unit.agent.act("Lurk")
            unit.agent.escalatePriority(TrafficPriorities.Bump) // Stronger than attackers, less than full retreaters
            combat.maneuvers.applyForce(Forces.target,   Potential.towardsTarget(unit))
            combat.maneuvers.applyForce(Forces.threat,   Potential.hardAvoidThreatRange(unit, Math.max(unit.topSpeed * framesToAttack, 64.0)))
            combat.maneuvers.applyForce(Forces.leaving,  Potential.towards(unit, unit.agent.safety) * combat.ground10)
            combat.maneuvers.moveForcefully()
          }
        }
      } else if (combat.distances.readyToApproachTarget || ! unit.matchups.wantsToVolley.contains(true) || KiteAway.exemptFromVolley(unit.totalHealth) || combat.shouldChase) {
        if ( ! kiteAway() && ! approach()) {
          unit.agent.act("Engage")
          combat.maneuvers.charge()
        }
      } else {
        unit.agent.act("Reposition")
        val urgency01 = Math.max(0.0, (unit.cooldownMaxAirGround - unit.cooldownLeft).toDouble / (unit.cooldownMaxAirGround))
        // Haven't tried this out; might help with sliding into formation
        // engageFormation.flatMap(_(unit)).foreach(f => applyForce(Forces.travel, Potential.towards(unit, f)))
        combat.maneuvers.applyForce(Forces.target,   Potential.towardsTarget(unit) * urgency01)
        combat.maneuvers.applyForce(Forces.threat,   Potential.softAvoidThreatRange(unit))
        combat.maneuvers.applyForce(Forces.leaving,  Potential.towards(unit, unit.agent.safety) * combat.ground10 * (1 - urgency01))
        combat.maneuvers.moveForcefully()
      }
    }
    unit.unready
  }
}
