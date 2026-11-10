package Tactic.Missions.drop

import Tactic.Missions.core.MissionDrop
import Tactic.Missions.routing.DropRoute
import Mathematics.Points.{Pixel, Tile}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Drop routing: per-transport flyby routes, arrival ranges and the next
 * waypoint toward a destination tile.
 */
trait DropRouting { self: MissionDrop =>

  private[Missions] final def approachStalled: Boolean =
    transports.nonEmpty && transports.forall(t => routes.get(t).exists(_.stalled))

  private[Missions] final def routeWaypoint(transport: FriendlyUnitInfo, destination: Tile): Option[Pixel] = {
    val route = routes.getOrElseUpdate(transport, new DropRoute)
    val approaching = (state == Travelling || state == Landing) && passengersFor(transport).exists(requiredPassenger)
    val arrivalRange = if (approaching) routeArrivalRange(transport) else 0.0
    val following = if (approaching) routeFollowing(transport) else Vector.empty
    route.next(transport.tile, destination, arrivalRange, following,
      preferUnseen = DropRoute.preferUnseen(transport), friendlyProximity = routeProximity).flatMap(route.waypoint(transport, _))
  }

  private[Missions] def intendRoute(transport: FriendlyUnitInfo, destination: Tile): Unit = {
    transport.intend(this).setTerminus(destination.center).setAction(ActionTravelTransport)
  }
}
