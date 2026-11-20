package Tactic.Missions.drops

import Tactic.Missions.core.MissionDrop
import Information.Geography.Types.Base
import Lifecycle.With
import Planning.ResourceLocks.LockUnits
import Utilities.UnitCounters.CountUpTo
import Utilities.UnitFilters.IsAll
import Utilities.UnitPreferences.PreferClose
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.SquadTargeting

/**
 * Dark Templar ferry: the moment a Shuttle and a Templar both exist,
 * fly the pair straight at the enemy main and drop it in the main
 * region as fast as the hulls allow. No sweep, no detector vetoes,
 * no waiting for a safer itinerary - the whole point is beating the
 * main-base cliff before the detectors settle. Single trip: once the
 * crew is out the ride is released and the mission ends.
 */
class MissionDarkTemplarFerry extends MissionDrop {

  override protected def additionalFormationConditions: Boolean =
    With.recruiter.available.exists(MatchFerryableDT) &&
      With.recruiter.available.exists(Protoss.Shuttle(_))

  object MatchFerryableDT extends IsAll(Protoss.DarkTemplar, recruitablePassenger)
  val dtLock: LockUnits = new LockUnits(this, MatchFerryableDT).setCounter(CountUpTo(2))

  override protected def additionalItineraryConditions(base: Base): Boolean =
    With.scouting.enemyMain.contains(base)

  override protected def recruit(): Unit = {
    populateItinerary()
    vicinity = itinerary.headOption.map(_.heart).getOrElse(With.scouting.enemyHome).center
    transportLock.setPreference(PreferClose(vicinity)).acquire()
    if (transportLock.units.isEmpty) { terminate("No transports available"); return }

    dtLock.setPreference(PreferClose(transportLock.units.head.pixel)).acquire()
    if (dtLock.units.isEmpty) { terminate("No DTs available"); return }

    transports ++= transportLock.units
    passengers ++= dtLock.units
  }

  override protected def readyToRaid(transport: FriendlyUnitInfo): Boolean =
    passengersFor(transport).nonEmpty &&
      transport.pixelDistanceCenter(landingGoal(transport)) <= 64

  override def raid(): Unit = {
    transportLock.release()
    transports.clear()
    SquadTargeting.targetRaid(this)
  }
}
