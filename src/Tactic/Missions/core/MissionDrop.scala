package Tactic.Missions.core

import Tactic.Missions.drop._
import Tactic.Missions.routing.DropRoute
import Debugging.SimpleString
import Information.Geography.Types.Base
import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.{Pixel, Tile}
import Micro.Actions.Basic.DoNothing
import Micro.Actions.{Action, Idle}
import Micro.Agency.Commander
import Micro.Coordination.Pathing.MicroPathing
import Planning.ResourceLocks.LockUnits
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Tactic.Squads.SquadAutomation
import Utilities.Time.Seconds
import Utilities.Time.Minutes
import Utilities.UnitCounters.CountOne
import Utilities.UnitFilters.IsWorker

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

abstract class MissionDrop extends Mission
  with DropLifecycle
  with DropPhases
  with DropManifestOps
  with DropRouting
  with DropTransportActions
  with DropPassengerActions {
  override def fightsIndependently: Boolean = launched
  trait DropState   extends SimpleString
  object Assembling extends DropState
  object Travelling extends DropState
  object Landing    extends DropState
  object Raiding    extends DropState
  object Evacuating extends DropState
  object Escaping   extends DropState

  protected def additionalFormationConditions             : Boolean
  protected def additionalItineraryConditions(base: Base) : Boolean = true
  protected def requireWorkers                            : Boolean = false
  protected def refreshItinerary(): Unit = {}
  protected def continueItinerary(from: Base): Boolean = false
  protected def isScoutingTarget(base: Base): Boolean = false
  protected def canLandAt(base: Base): Boolean = true
  protected def targetCleared(base: Base): Boolean = requireWorkers && base.heart.visible && base.units.forall(u => !u.isEnemy || !IsWorker(u))
  protected def shouldStopRaiding                         : Boolean = false
  protected def shouldGoHome                              : Boolean = false
  protected def canContinueAfterLeaving                   : Boolean = true
  protected def recruited(unit: UnitInfo)                 : Boolean = With.recruiter.lockedBy(this).exists(_ == unit)
  protected def recruitable(unit: UnitInfo)               : Boolean = unit.complete && (recruited(unit) || ! unit.team.exists(_.engagedUpon))
  protected def recruitablePassenger(unit: UnitInfo)      : Boolean = recruitable(unit) && (recruited(unit) || Maff.orElse(transportLock.units, With.recruiter.available.filter(transportLock.matcher)).exists(_.pixelDistanceCenter(unit) < 640))
  protected def raidTransportAction: Action = ActionRaidTransport
  protected def commitWhileRaiding: Boolean = true
  protected def defaultCombatLanding: Boolean = false
  protected def requiredPassenger(passenger: FriendlyUnitInfo): Boolean = true
  protected def landingGoal(transport: FriendlyUnitInfo): Pixel = vicinity
  protected def readyToRaid(transport: FriendlyUnitInfo): Boolean = false
  protected def onRaidHandoff(transport: FriendlyUnitInfo): Unit = {}
  def requestsUnload(passenger: FriendlyUnitInfo): Boolean = false
  def allowsPickup(transport: FriendlyUnitInfo, passenger: FriendlyUnitInfo): Boolean = true
  protected def rejectApproach(transport: FriendlyUnitInfo): Boolean = false
  protected def routeArrivalRange(transport: FriendlyUnitInfo): Double = 0
  protected def routeProximity: Option[Double] = None
  protected def safeToRelease(transport: FriendlyUnitInfo): Boolean = true
  protected def routeFollowing(transport: FriendlyUnitInfo): Vector[(Tile, Double)] = Vector.empty
  protected final def threatenedOnApproach(transport: FriendlyUnitInfo, waypoint: => Option[Pixel]): Boolean =
    transport.matchups.threats.nonEmpty && !transport.matchups.withinSafetyMargin &&
      !waypoint.exists(MicroPathing.isSafeMove(transport, _))
  protected def onDestinationChanged(): Unit = {}
  protected def missionTimeout: Int = Seconds(90)()
  protected def approachTimeout: Int = Seconds(45)()
  protected val transportLock: LockUnits = new LockUnits(this, u => u.isTransport && recruitable(u)).setCounter(CountOne).setInterruptible(false)
  private[Missions] val cargoLock = new LockUnits(this).setInterruptible(false)

  val itinerary                             = new mutable.Queue[Base]
  val transports                            = new ArrayBuffer[FriendlyUnitInfo]
  val passengers                            = new ArrayBuffer[FriendlyUnitInfo]
  var state             : DropState         = Assembling
  private[Missions] val routes = mutable.Map.empty[FriendlyUnitInfo, DropRoute]
  private[Missions] val initiallyLanded = mutable.Set.empty[FriendlyUnitInfo]
  protected val raidingTransports = mutable.Set.empty[FriendlyUnitInfo]
  private[Missions] val pickupTargets = mutable.Map.empty[FriendlyUnitInfo, FriendlyUnitInfo]
  protected var destinationBase: Option[Base] = None
  protected var returningHome = false
  private[Missions] var itineraryFrame = 0
  private[Missions] var destinationFrame = 0

  protected def missionExpired: Boolean =
    With.framesSince(Math.max(launchFrame, itineraryFrame)) > missionTimeout ||
      (With.framesSince(Math.max(launchFrame, destinationFrame)) > approachTimeout && state != Raiding && state != Evacuating)

  final def assignedTransport(passenger: FriendlyUnitInfo): Option[FriendlyUnitInfo] =
    passenger.transport.filter(transports.contains).orElse(passenger.agent.ride.filter(transports.contains))
  protected final def passengersFor(transport: FriendlyUnitInfo): Seq[FriendlyUnitInfo] = passengers.filter(p => assignedTransport(p).contains(transport))

  final def abandonUnsafePickups(): Unit = {
    val abandoned = passengers.filter(p => !p.loaded && assignedTransport(p).exists(t => !allowsPickup(t, p))).toSet
    transports.filter(t => abandoned.exists(t.orderTarget.contains)).foreach(Commander.stop)
    releasePassengers(abandoned)
  }

  override def reset(): Unit = {
    itinerary.clear()
    transports.clear()
    passengers.clear()
    state = Assembling
    routes.clear()
    initiallyLanded.clear()
    raidingTransports.clear()
    pickupTargets.clear()
    destinationBase = None
    returningHome = false
    itineraryFrame = With.frame
    destinationFrame = With.frame
  }

  final override protected def shouldForm: Boolean = {
    With.blackboard.wantToHarass() && With.recruiter.available.exists(transportLock.matcher) && additionalFormationConditions && { populateItinerary(); itinerary.nonEmpty }
  }

  protected def populateItinerary(): Unit = {
    val eligibleBases = With.geography.bases.view
      .filter(_.mineralsLeft > 1500)
      .filterNot(ignore)
      // COG 2022: Bots react too fast to the drop (and often cannon up)
      .filter(b => With.configuration.humanMode || ! (With.scouting.enemyMain.contains(b) || With.scouting.enemyNatural.contains(b)))
    val bases = Maff.orElse(eligibleBases.filter(_.owner.isEnemy), eligibleBases).toVector
    if (bases.isEmpty) return

    val targetBase = Maff.sampleWeighted[Base](bases, b =>
      if (b.owner.isNeutral) Maff.nanToOne(
        With.framesSince(b.lastFrameScoutedByUs).toDouble
        * Math.min(b.heart.groundTiles(With.scouting.enemyMuscleOrigin),  10 * b.heart.tileDistanceFast(With.scouting.enemyMuscleOrigin))
        / Math.min(b.heart.groundTiles(With.scouting.enemyHome),          10 * b.heart.tileDistanceFast(With.scouting.enemyHome)))
      else if (With.scouting.enemyMain.contains(b))     if ( ! b.owner.isZerg && With.frame > Minutes(13)()) 0 else 16 * (With.scouting.enemyProximity + 1)
      else if (With.scouting.enemyNatural.contains(b))  if ( ! b.owner.isZerg && With.frame > Minutes(16)()) 0 else 16 * (With.scouting.enemyProximity - 0.5)
      else Math.min(b.heart.groundTiles(With.scouting.enemyMuscleOrigin), 10 * b.heart.tileDistanceFast(With.scouting.enemyMuscleOrigin))).get
    val itineraries = Vector(
        With.geography.itineraryCounterwise(With.geography.ourMain, targetBase),
        With.geography.itineraryClockwise(With.geography.ourMain, targetBase))
      .sortBy(_.size)
      .sortBy(_.count(_.owner.isEnemy))
    itinerary.clear()
    itinerary ++= itineraries.head.filter(bases.contains)
    With.logger.debug(f"$this itinerary: ${itinerary.view.map(_.toString).mkString(" -> ")}")
  }

  final override def run(): Unit = {
    transports --= transports.filterNot(u => u.alive && u.squad.contains(this))
    passengers --= passengers.filterNot(u => u.alive && u.squad.contains(this))
    if (passengers.isEmpty && (!defaultCombatLanding || transports.isEmpty)) { finishDrop("No passengers left"); return }

    if (transports.isEmpty) {
      finishDrop("No transports left"); return
    }

    updateManifest()
    abandonUnsafePickups()
    if (passengers.isEmpty && !defaultCombatLanding) { finishDrop("No transportable passengers left"); return }

    if (!returningHome && !shouldGoHome) refreshItinerary()
    while (!returningHome && !shouldGoHome && itinerary.headOption.exists(skipBase)) {
      With.logger.debug(f"$this: Leaving target ${itinerary.head.heart}. Next target: ${itinerary.drop(1).headOption.map(_.heart)}")
      itinerary.dequeue()
    }
    val expired = missionExpired
    val renewed = renewItinerary(expired)
    updateState(shouldGoHome || (expired && !renewed))
    if (finishWithdrawal()) return

    transports.foreach(_.intend(this).setStation(None))
    state match {
      case Assembling => assemble()
      case Travelling => travel()
      case Landing    => land()
      case Raiding    => raid()
      case Evacuating => evacuate()
      case Escaping   => escape()
    }
  }

  protected def raid(): Unit = {
    SquadAutomation.targetRaid(this)
    transports.foreach(t => {
      if (defaultCombatLanding && !passengersFor(t).exists(requiredPassenger)) intendRoute(t, t.agent.defaultHome.tile)
      else t.intend(this).setTerminus(vicinity).setAction(raidTransportAction)
    })
    passengers.foreach(_.intend(this).setAction(Idle).setTerminus(vicinity))
    passengers.foreach(_.agent.commit = commitWhileRaiding)
  }
}
