package Micro.Agency

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.{Pixel, Points, Tile}
import Micro.Coordination.Pathing.MicroPathing
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.?

/** See Commander: role-grouped delegates behind the facade. */
object CommanderOrder {

  private[Micro] def findGroundWaypointWithClearance(unit: FriendlyUnitInfo, waypoint: Pixel): Option[Pixel] = {

    val kind = unit.unitClass

    def hasClearance(p: Pixel): Boolean = {

      val topLeft = p.add(kind.topLeft)

      val bottomRight = p.add(kind.bottomRightInclusive)

      val from = topLeft.subtract(1, 1).tile

      val to = bottomRight.add(1, 1).tile

      if ( ! from.valid || ! to.valid) return false



      val innerFrom = topLeft.tile

      val innerTo = bottomRight.tile

      var y = from.y

      while (y <= to.y) {

        var x = from.x

        while (x <= to.x) {

          val i = x + y * With.mapTileWidth

          if (With.grids.unwalkableUnits.isSetUnchecked(i) || (x >= innerFrom.x && x <= innerTo.x && y >= innerFrom.y && y <= innerTo.y && ! With.grids.walkableTerrain.getUnchecked(i))) return false



          x += 1

        }

        y += 1

      }

      true

    }

    if (hasClearance(waypoint)) return Some(waypoint)



    val center = waypoint.walkableTile.center

    if (hasClearance(center)) return Some(center)



    var dy = -1

    while (dy <= 1) {

      var dx = -1

      while (dx <= 1) {

        val candidate = center.add(dx, dy)

        if (hasClearance(candidate)) return Some(candidate)



        dx += 1

      }

      dy += 1

    }

    None

  }



  /**

    * Adjust the unit's destination to ensure successful execution of the move.

    */

  // Planning queries leave fallback state unchanged; command methods record it when ready.


  private[Micro] def adjustDestination(unit: FriendlyUnitInfo, argTo: Pixel): Pixel = adjustDestination(unit, argTo, remember = false)



  private[Micro] def adjustDestination(unit: FriendlyUnitInfo, argTo: Pixel, remember: Boolean): Pixel = {

    if ( ! unit.intent.shouldAdjustXY) return argTo



    var to: Pixel = argTo



    // Send some units past their destination to maximize acceleration

    if (With.reaction.sluggishness == 0 && (unit.isAny(Terran.ScienceVessel, Protoss.Observer, Protoss.Shuttle, Zerg.Mutalisk, Zerg.Overlord, Zerg.Queen) || unit.loadedUnits.nonEmpty)) {

      val overshootDistance = ?(unit.airborne, 288.0, 8)

      if (to == unit.pixel) {

        val signX = Maff.signum11(Points.middle.x - to.x)

        val signY = Maff.signum11(Points.middle.y - to.y)

        to = to.add((signX * overshootDistance).toInt, (signY * overshootDistance).toInt)

      } else if (unit.pixelDistanceSquared(to) < overshootDistance * overshootDistance) {

        to = unit.pixel.project(to, overshootDistance)

      }

    }



    // Clip to map

    to = Pixel(

      Maff.clamp(to.x, unit.unitClass.dimensionLeft, With.mapPixelWidth  - unit.unitClass.dimensionRightExclusive),

      Maff.clamp(to.y, unit.unitClass.dimensionUp,   With.mapPixelHeight - unit.unitClass.dimensionDownExclusive))



    if (unit.airborne) return to



    // If no collision-safe local waypoint exists, keep the farther destination as the

    // order target (subject to the corrections below). BW computes the path to that target.

    val distance = unit.pixelDistanceTravelling(to)

    if (distance >= 2 * MicroPathing.waypointDistancePixels && With.reaction.sluggishness < 2 && unit.zone != to.zone) {

      // Each unit stores (destination pixel, estimated ground distance on entry minus 128 pixels).

      // Recompute distance from its current position on each call. Retain fallback only when

      // the destination coordinates match exactly and distance exceeds the saved threshold.

      // Otherwise retry local waypointing; movement back and forth does not accumulate progress.

      val fallback = unit.agent.waypointFallback.filter { case (goal, resumeAt) => goal == to && distance > resumeAt }

      if (fallback.isEmpty) {

        val waypoint = MicroPathing.tryGetGroundWaypointToPixel(unit.pixel, to).flatMap(findGroundWaypointWithClearance(unit, _))

        if (remember) unit.agent.waypointFallback = if (waypoint.isDefined) None else Some((to, distance - MicroPathing.waypointDistancePixels))

        to = waypoint.getOrElse(to.walkablePixel)

      }

    } else if (remember) {

      unit.agent.waypointFallback = None

    }



    // Even slight intersection with a non-walkable tile can cause a unit to refuse movement

    // This could prevent getting stuck eg when trying to move behind a mineral line with a Dragoon

    var walkabilityAssured = to.walkable

    walkabilityAssured &&= to.add( - unit.unitClass.dimensionLeft,            - unit.unitClass.dimensionUp            ).walkable

    walkabilityAssured &&= to.add(   unit.unitClass.dimensionRightInclusive,    unit.unitClass.dimensionDownInclusive ).walkable

    walkabilityAssured &&= to.add(   unit.unitClass.dimensionRightInclusive,  - unit.unitClass.dimensionUp            ).walkable

    walkabilityAssured &&= to.add( - unit.unitClass.dimensionLeft,              unit.unitClass.dimensionDownInclusive ).walkable

    if ( ! walkabilityAssured && unit.pixelDistanceCenter(to) > 32) {

      to = to.walkableTile.center

    }



    // Apply noise

    //

    // According to https://github.com/tscmoo/tsc-bwai/commit/ceb13344f5994d28d6b601cef126f264ca97426b

    // ordering moves to the exact same destination causes Brood War to not recalculate the path.

    //

    // That means that if the unit got confused while executing the original order, it will remain confused.

    // Issuing an order to a slightly different position can cause Brood War to recalculate the path and un-stick the unit.

    //

    // However, this recalculation can itself sometimes cause units to get stuck on obstacles.

    // Specifically, units tend to get stuck on buildings this way.

    // The neutral buildings on Roadrunner frequently cause this.

    //

    // So we'll try to get the best of both worlds, and recalculate paths *occasionally*

    // and only at distances long enough to avoid the issue.

    //

    if (unit.pixelDistanceCenter(to) > 160) {

      to = to.add((unit.id + With.frame / 96) % 3 - 1, 0)

    }

    to

  }



}
