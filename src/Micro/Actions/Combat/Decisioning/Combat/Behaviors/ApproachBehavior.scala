package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Micro.Actions.Combat.Decisioning.Formation.{FormationApproach, OverwhelminglyStronger}
import Micro.Actions.Combat.Decisioning.Skirmish.FightRules
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.?

/**
 * Approach behavior (was CombatEngagement.approach): the formation approach
 * governor dresses the line before contact instead of trickling in
 * piecemeal. Only while contact is lost; real contact fights normally.
 * Melee must still close, flankers still circle, yolo still overrules, and
 * without a formation there is nothing to dress. Returns true when it moved
 * or held the unit.
 */
object ApproachBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! approachEligible(unit)) return false

    dressLine(unit, combat)
  }

  private def approachEligible(unit: FriendlyUnitInfo): Boolean = {
    if (With.yolo.active) return false
    if (unit.unitClass.melee) return false
    if (unit.formationEngage.isEmpty || unit.formationEngage.exists(_.flanking)) return false

    val nearest = unit.matchups.targetNearest
    if (nearest.isEmpty) return false

    val squadUnits = unit.squad.map(_.units).getOrElse(Seq(unit))
    if (squadUnits.exists(_.canAnyEnemyAttackUsWithMargin(FightRules.contactMarginTiles))) return false
    if (OverwhelminglyStronger.skipFormation(unit)) return false

    true
  }

  private def dressLine(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    val posture = approachPosture
    val stats = unit.squad.flatMap(_.lineStats)
    val ownBeyond = unit.pixelsToGetInRange(unit.matchups.targetNearest.get) / 32.0
    val order = FormationApproach.decide(ownBeyond, stats, posture)
    if (With.configuration.formationAudit) {
      Debugging.FormationAudit.noteDress(
        With.frame,
        unit.id,
        unit.unitClass.toString,
        ownBeyond,
        stats.map(_.meanTiles).getOrElse(Double.NaN),
        stats.map(_.stddevTiles).getOrElse(Double.NaN),
        stats.map(_.count).getOrElse(0),
        posture.toString,
        order.toString)
    }
    order match {
      case FormationApproach.FreeMarch | FormationApproach.BreakThrough => false
      case FormationApproach.HoldLine => holdLine(unit)
      case FormationApproach.StepForward | FormationApproach.CloseCautious => stepForward(unit, combat)
      case FormationApproach.StepBack => stepBack(unit, combat)
    }
  }

  private def holdLine(unit: FriendlyUnitInfo): Boolean = {
    unit.agent.act("Dress")
    Commander.hold(unit)

    true
  }

  private def stepForward(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    unit.agent.act("Dress")
    combat.maneuvers.move()

    true
  }

  private def stepBack(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    unit.agent.act("Dress")
    if (With.frame % FormationApproach.correctionCadence == unit.id % FormationApproach.correctionCadence) {
      unit.agent.decision.set(unit.pixel.project(unit.matchups.targetNearest.get.pixel, -FormationApproach.correctionStepTiles * 32))
      combat.maneuvers.move()
    } else {
      Commander.hold(unit)
    }

    true
  }

  private def approachPosture: FormationApproach.ApproachPosture =
    ?(With.enemies.exists(_.isTerran), FormationApproach.Decisive, FormationApproach.Cautious)
}
