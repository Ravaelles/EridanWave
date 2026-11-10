package Tactic.Missions.drops

import Tactic.Missions.core.MissionDrop
import Lifecycle.With
import Mathematics.Points.Pixel
import Micro.Actions.{Action, Idle}
import Micro.Agency.Commander
import Planning.ResourceLocks.LockUnits
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.SquadAutomation
import Utilities.UnitCounters.CountUpTo
import Utilities.UnitPreferences.PreferClose

import scala.collection.mutable

/**
 * Ferry: stage at the vanguard and shuttle the army to the front a few
 * units at a time, hunting tanks first (Stardust ShuttleHarass doctrine).
 * Unlike single-trip drops the mission never ends at the front: the crew
 * fights, the Shuttle returns for the waiting pool, fresh troops top up
 * at staging, and delivered fighters stay fighters (excluded from pickup
 * by requiredPassenger, never re-boarded).
 */
class MissionFerry extends MissionDrop {

  override protected def additionalFormationConditions: Boolean =
    With.blackboard.ferry() &&
      With.recruiter.available.exists(transportLock.matcher) &&
      With.recruiter.available.count(u => u.isAny(Protoss.Dragoon, Protoss.Zealot) && recruitablePassenger(u)) >= 4

  val goonLock: LockUnits = new LockUnits(this, u => Protoss.Dragoon(u) && recruitablePassenger(u)).setCounter(CountUpTo(6))
  val zealotLock: LockUnits = new LockUnits(this, u => Protoss.Zealot(u) && recruitablePassenger(u))
  val ferryLock: LockUnits = new LockUnits(this, u => u.isAny(Protoss.Dragoon, Protoss.Zealot) && recruitablePassenger(u)).setCounter(CountUpTo(6))

  private val delivered = new mutable.HashSet[Int]

  override protected def requiredPassenger(passenger: FriendlyUnitInfo): Boolean =
    passenger.isAny(Protoss.Dragoon, Protoss.Zealot) && ! delivered(passenger.id)

  override protected def defaultCombatLanding: Boolean = true

  override protected def recruit(): Unit = {
    populateItinerary()
    vicinity = itinerary.headOption.map(_.heart).getOrElse(With.scouting.enemyHome).center
    transportLock.setCounter(CountUpTo(2)).setPreference(PreferClose(vicinity)).acquire()
    if (transportLock.units.isEmpty) { terminate("No transports available"); return }

    val anchor = transportLock.units.head.pixel
    goonLock.setPreference(PreferClose(anchor)).acquire()
    zealotLock.setCounter(CountUpTo(Math.max(0, 6 - goonLock.units.size))).setPreference(PreferClose(anchor)).acquire()
    if (goonLock.units.isEmpty && zealotLock.units.isEmpty) { terminate("No ferry crew available"); return }

    transports ++= transportLock.units
    passengers ++= goonLock.units
    passengers ++= zealotLock.units
  }

  override protected def landingGoal(transport: FriendlyUnitInfo): Pixel =
    frontGoal(transport)

  override protected def readyToRaid(transport: FriendlyUnitInfo): Boolean =
    destinationBase.exists(_.isEnemy) &&
      passengersFor(transport).exists(requiredPassenger) &&
      transport.pixelDistanceCenter(landingGoal(transport)) <= 64

  override protected def onRaidHandoff(transport: FriendlyUnitInfo): Unit = {
    vicinity = landingGoal(transport)
  }

  override protected def missionExpired: Boolean =
    transports.isEmpty

  override def raid(): Unit = {
    SquadAutomation.targetRaid(this)
    passengers.foreach(p => if ( ! p.loaded) p.intend(this).setAction(Idle).setTerminus(vicinity))
    passengers.foreach(_.agent.commit = commitWhileRaiding)
    topUpCrew()
    transports.foreach(ferryCycle)
  }

  private def stagingPoint: Pixel =
    With.scouting.ourMuscleOrigin.center

  private def frontGoal(transport: FriendlyUnitInfo): Pixel = {
    val tanks = With.units.enemy.filter(u =>
      u.isAny(Terran.SiegeTankSieged, Terran.SiegeTankUnsieged) && u.likelyStillThere)
    val scored = tanks.flatMap(tank => {
      val dist = transport.pixelDistanceCenter(tank.pixel)
      FerryTargetScoring.score(
        dist,
        Terran.SiegeTankSieged(tank),
        tank.totalHealth.toDouble / tank.unitClass.maxTotalHealth,
        dist > FerryTargetScoring.airThreatRangePixels && With.grids.enemyRangeAir.get(tank.tile) > 0,
        With.grids.enemyRangeGround.get(tank.tile)).map(tank.pixel -> _)
    })
    scored.toVector.sortBy(_._2).headOption.map(_._1).getOrElse(fallbackGoal(transport))
  }

  private def fallbackGoal(transport: FriendlyUnitInfo): Pixel = {
    val warriors = With.units.enemy.filter(u =>
      u.likelyStillThere && ! u.flying && u.canAttackGround &&
      u.pixelDistanceCenter(transport) < 32 * 40)
    warriors.toVector.sortBy(transport.pixelDistanceCenter).headOption.map(_.pixel).getOrElse(vicinity)
  }

  private def topUpCrew(): Unit = {
    if (transports.isEmpty) return
    if (transports.exists(t => passengersFor(t).filter(_.loaded).nonEmpty)) return

    val anchor = transports.head.pixel
    ferryLock.setPreference(PreferClose(anchor)).acquire()
    val fresh = ferryLock.units.filterNot(u => passengers.contains(u) || delivered(u.id))
    if (fresh.nonEmpty) {
      passengers ++= fresh
    }
  }

  private def ferryCycle(transport: FriendlyUnitInfo): Unit = {
    val pool = passengersFor(transport)
    pool.filterNot(_.loaded).foreach(p => delivered += p.id)
    val aboard = pool.filter(_.loaded)
    if (aboard.nonEmpty) {
      val goal = frontGoal(transport)
      if (transport.pixelDistanceCenter(goal) <= 64) {
        aboard.sortBy(_.id).headOption.foreach(p => Commander.unload(transport, p))
      } else {
        transport.intend(this).setTerminus(goal).setAction(ActionTravelTransport)
      }
    } else {
      val staging = stagingPoint
      if (transport.pixelDistanceCenter(staging) > 384) {
        transport.intend(this).setTerminus(staging).setAction(ActionTravelTransport)
      } else {
        transport.intend(this).setAction(ActionEvacuateTransport)
      }
    }
  }
}
