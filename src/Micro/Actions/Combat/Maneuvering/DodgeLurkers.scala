package Micro.Actions.Combat.Maneuvering

import Micro.Actions.Action
import Micro.Actions.Combat.Decisionmaking.LurkerSafety
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Lurker dodge, ported from Atlantis AvoidLurkers: when LurkerSafety says
 * the unit cannot fight the Lurkers around it, step 2.5 tiles away from
 * the deepest one instead of standing in the spines.
 */
object DodgeLurkers extends Action {

  override def allowed(unit: FriendlyUnitInfo): Boolean =
    ! unit.flying && unit.canMove && ! unit.unitClass.isBuilding && exposedToLurkers(unit)

  /** Undetected Dark Templar hold position; the wounded never trust it (a hidden detector may cover them). */
  private def exposedToLurkers(unit: FriendlyUnitInfo): Boolean =
    ! unit.effectivelyCloaked || unit.totalHealth <= 60

  override def perform(unit: FriendlyUnitInfo): Unit = {
    if (unit.ready) {
      val radius = LurkerSafety.avoidanceRadiusTiles(unit)
      val threat = (LurkerSafety.burrowingNear(unit, radius) ++ LurkerSafety.lurkersToAvoid(unit, radius))
        .sortBy(_.pixelDistanceSquared(unit.pixel))
        .headOption

      if (LurkerSafety.shouldAvoid(unit) && threat.isDefined) {
        unit.agent.decision.set(unit.pixel.project(threat.get.pixel, -2.5 * 32).clamp())
        unit.agent.act("DodgeLurker")
        Commander.move(unit)
      }
    }
    unit.unready
  }
}
