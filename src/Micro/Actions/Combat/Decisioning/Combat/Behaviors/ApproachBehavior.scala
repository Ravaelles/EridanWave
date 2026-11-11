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
    if (With.yolo.active) return false
    if (unit.unitClass.melee) return false
    if (unit.formationEngage.isEmpty || unit.formationEngage.exists(_.flanking)) return false

    val nearest = unit.matchups.targetNearest
    if (nearest.isEmpty) return false

    val squadUnits = unit.squad.map(_.units).getOrElse(Seq(unit))
    if (squadUnits.exists(_.canAnyEnemyAttackUsWithMargin(FightRules.contactMarginTiles))) return false
    if (OverwhelminglyStronger.skipFormation(unit)) return false

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

  private def approachPosture: FormationApproach.ApproachPosture =
    ?(With.enemies.exists(_.isTerran), FormationApproach.Decisive, FormationApproach.Cautious)
}
