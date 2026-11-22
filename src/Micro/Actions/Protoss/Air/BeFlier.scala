package Micro.Actions.Protoss.Air

import Micro.Actions.UnitControl
import Debugging.Visualizations.Forces
import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Actions.Combat.Maneuvering.AirStaticStandoff
import Micro.Agency.Commander
import Micro.Coordination.Pathing.MicroPathing
import Micro.Heuristics.Potential
import ProxyBwapi.Races.Zerg
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object BeFlier extends Action {
  override def isContainer: Boolean = true

  /** Flier steering: dodge Scourge toward friendly AA and nearby safety.
    * Transports have their own discipline (Transport -> BeShuttle); steering
    * them here too would give a Shuttle two movement owners in one frame. */
  override def allowed(unit: FriendlyUnitInfo): Boolean = unit.airborne && unit.canMove && ! unit.unitClass.isBuilding && ! unit.isTransport

  override protected def perform(unit: FriendlyUnitInfo): UnitControl = {
    // Static guns first, always: no flyer ever trades inside a gun
    // building's keep-off, and the jittered leg breaks shared-ray mills.
    if (AirStaticStandoff.act(unit)) return UnitControl.claimedIf(! unit.ready)

    val scourge = unit.matchups.threats
      .filter(Zerg.Scourge)
      .filter(_.pixelDistanceEdge(unit) < Math.max(96, if (unit.canAttackAir) 32 + unit.pixelRangeAir else 9))
      .toVector

    if (scourge.nonEmpty) {

      // TODO: Sniping scourge that aren't facing us
      // TODO: Triangle juking of scourge

      unit.agent.forces(Forces.threat) = Potential.towardsUnit(unit, scourge.minBy(_.pixelDistanceEdge(unit)), -1)

      val safety = Maff.minBy(With.units.ours.view.filter(u => u.canAttackAir && ! u.flying))(_.pixelDistanceEdge(unit))
      safety.foreach(s => unit.agent.forces(Forces.regrouping) = Potential.towardsUnit(unit, s, 1))

      val waypoint = MicroPathing.getWaypointInDirection(unit, unit.agent.forces.sum.radians)
      if (waypoint.isDefined) {
        unit.agent.forceTo(waypoint)
        Commander.move(unit)
      }
    }
    UnitControl.claimedIf(! unit.ready)
  }

}
