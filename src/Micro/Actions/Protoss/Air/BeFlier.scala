package Micro.Actions.Protoss.Air

import Debugging.Visualizations.Forces
import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Actions.Combat.Tactics.Potshots.FewShot
import Micro.Agency.Commander
import Micro.Coordination.Pathing.MicroPathing
import Micro.Heuristics.Potential
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
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
    if ( ! Protoss.Shuttle(unit)) return false

    if (unit.loadedUnits.forall(u => ! Protoss.Reaver(u))) return false

    // Typed scan, not threats: Scourge carries no weapon in the data and
    // never lands in matchups. Engine pixels, threshold in tiles (rule 10).
    val scourgeNear = ProxyBwapi.UnitTracking.AliveEnemies.get().exists(u =>
      Zerg.Scourge(u) && u.likelyStillThere &&
        u.tileDistanceEdge(unit) < Micro.Actions.Protoss.Shuttle.ShuttleFleeGeometry.scourgeFleeDist)
    if ( ! scourgeNear) return false

    val members = unit.squad.map(_.units.map(_.pixel))
      .getOrElse(unit.loadedUnits.map(_.pixel) :+ unit.pixel)
    val rear = Micro.Actions.Protoss.Shuttle.ShuttleFleeGeometry.rearOfCluster(
      members,
      With.geography.ourMain.heart.center)
    unit.agent.act("FleeScourge")
    unit.agent.destinationNext.set(rear)
    Commander.move(unit)
    true
  }
}
