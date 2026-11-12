package Micro.Actions.Protoss.Air

import Debugging.Visualizations.Forces
import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Actions.Combat.Tactics.Potshots.FewShot
import Micro.Actions.Protoss.Shuttle.ShuttleScourgeFlee
import Micro.Agency.Commander
import Micro.Coordination.Pathing.MicroPathing
import Micro.Heuristics.Potential
import ProxyBwapi.Races.{Terran, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object BeFlier extends Action {

  override def allowed(unit: FriendlyUnitInfo): Boolean = unit.airborne && unit.canMove && ! unit.unitClass.isBuilding

  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    if (fleeScourgeToRear(unit)) return

    val scourge = unit.matchups.threats
      .filter(Zerg.Scourge)
      .filter(_.pixelDistanceEdge(unit) < Math.max(96, if (unit.canAttackAir) 32 + unit.pixelRangeAir else 9))
      .toVector

    if (scourge.nonEmpty) {

      if ( ! Terran.Valkyrie(unit) && (unit.unitClass.dealsRadialSplashDamage || scourge.length == 1)) {
        // Not actually be implemented yet
        FewShot(unit, Zerg.Scourge, 1)
      }

      // TODO: Sniping scourge that aren't facing us
      // TODO: Triangle juking of scourge

      unit.agent.forces(Forces.threat) = Potential.towardsUnit(unit, scourge.minBy(_.pixelDistanceEdge(unit)), -1)

      val safety = Maff.minBy(With.units.ours.view.filter(u => u.canAttackAir && ! u.flying))(_.pixelDistanceEdge(unit))
      safety.foreach(s => unit.agent.forces(Forces.regrouping) = Potential.towardsUnit(unit, s, 1))

      val waypoint = MicroPathing.getWaypointInDirection(unit, unit.agent.forces.sum.radians)
      if (waypoint.isDefined) {
        unit.agent.destinationNext.set(waypoint)
        Commander.move(unit)
      }
    }
  }

  /**
   * A Shuttle carrying Reavers runs at twelve tiles toward the rear of
   * its cluster; at the generic three the Scourge are already on it.
   * Returns true when it took over movement.
   */
  private def fleeScourgeToRear(unit: FriendlyUnitInfo): Boolean = {
    val rear = ShuttleScourgeFlee.fleePixel(unit)
    rear.foreach(p => {
      unit.agent.act("FleeScourge")
      unit.agent.destinationNext.set(p)
      Commander.move(unit)
    })

    rear.isDefined
  }
}
