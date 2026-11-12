
package Micro.Agency

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Physics.{ForceMap, ForceMath}
import Mathematics.Points.Pixel
import Micro.Actions.Combat.Decisioning.Combat
import Micro.Coordination.Pushing.{TrafficPriorities, TrafficPriority}
import Performance.{Cache, KeyedCache}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

class Agent(val unit: FriendlyUnitInfo) extends DestinationStack with AgencySortOrder {

  /////////////
  // History //
  /////////////

  var lastFrame     : Int = 0
  var lastStim      : Int = 0
  var lastCloak     : Int = 0
  var run           : Int = 0
  var tryingToMove  : Boolean = false // TODO: Can we infer this from destinations?
  var waypointFallback: Option[(Pixel, Double)] = None // (Destination pixel before waypoint substitution, remaining ground-distance threshold for retrying)

  ///////////////
  // Decisions //
  ///////////////

  var toNuke        : Option[Pixel]             = None
  var toAttack      : Option[UnitInfo]          = None
  var toAttackLast  : Option[UnitInfo]          = None
  var toGather      : Option[UnitInfo]          = None
  var toBoard       : Option[FriendlyUnitInfo]  = None
  var shouldFight   : Boolean                   = false
  var commit        : Boolean                   = false
  var wantsUnload   : Boolean                   = false
  var wantsPickup   : Boolean                   = false
  var priority      : TrafficPriority           = TrafficPriorities.Freedom
  val forces        : ForceMap                  = new ForceMap
  val combat        : Combat                    = new Combat(unit)

  /////////////////
  // Diagnostics //
  /////////////////

  var fightReason : String           = ""
  var lastAction  : Option[String]   = None
  val actions     : ArrayBuffer[String] = new ArrayBuffer[String]()

  /**
   * Chain containers credit themselves every frame via Action.apply, so
   * their names would drown the trace (half a million Idle rows a game).
   * Only leaf behaviors report; extend this when adding a chain container.
   */
  private val chainActions: Set[String] = Set(
    "Cancel", "Liftoff", "Land", "Unbunk", "Meld", "Build",
    "FinishConstruction", "FightOrFlee", "Repair", "GetRepairedMobile",
    "GetRepairedBuilding", "Gather", "Addon", "Scan", "Unstick", "Produce",
    "Rally", "ReloadInterceptors", "ReloadScarabs", "Transport", "Scout",
    "Bunk", "Crack", "Fight", "Attack", "Travel", "Follow", "StrategicNuke",
    "Cast", "Spot", "Stim", "EmergencyBunk", "Root", "BeFlier",
    "BeCombatSCV", "BeVulture", "BeRushingZealot", "BeZealotVsZealot",
    "BeDragoonVsSlowlot", "BeDarkTemplar", "BeCarrier", "BeArbiter",
    "BeReaver", "BeMutalisk", "Recharge", "Bust", "Stealth", "OccupyBunker",
    "BeDetector", "Evacuate", "BeShuttle", "CannonRush", "PylonTrap",
    "FightWithZealot", "DroneWarfare", "Harass4PoolWorkers", "SabotageProxy",
    "KnockKnock", "PreserveScout", "AttackBuilder", "BlockConstruction",
    "Search", "SearchWhenBored", "Idle", "DoNothing")

  def act(value: String): Unit = {
    actions += value
    lastAction = Some(value)
  }

  // One row per frame: the leaf decisions chained as A/B/C instead of one
  // value, so the logs show the reasoning path, not just its end. Chain
  // containers stay out (half a million Idle rows a game); the change-only
  // channel still dedups steady chains. Probe mirrors the same label for
  // games without a trace (UMS): arm keys in bwapi-data/write/probe.txt.
  private def flushDecisionChain(): Unit = {
    val chain = actions.filterNot(chainActions).distinct
    if (chain.nonEmpty) {
      val label = chain.mkString("/")
      Debugging.CherryVis.CherryVis.decision(unit.id, label)
      Debugging.Probe.note("Act", unit.id, label)
      Debugging.DeathWatch.recordBehavior(unit.id, label)
    }
  }

  ///////////////
  // Execution //
  ///////////////

  def execute(): Unit = {
    if ( ! unit.isFriendly) return // Mind Control

    forces.clear()
    resetDestinations()

    run           += 1
    lastFrame     = With.frame
    toAttackLast  = toAttack
    priority      = TrafficPriorities.Freedom
    fightReason   = ""
    tryingToMove  = false
    wantsUnload   = false
    wantsPickup   = false
    flushDecisionChain()
    actions.clear()
    unit.orderTarget.foreach(_.removeDamage(unit))

    _rideGoal = None
    ride.filterNot(_.alive).foreach(_.agent.removePassenger(unit))
    passengers.view.filter(u => ! u.alive || ! u.isOurs || u.unitClass.isBuilding).foreach(removePassenger)
    val passengersReserved = reservedPassengers
    unit.loadedUnits.filterNot(passengersReserved.contains).foreach(addPassenger)

    toAttack  = unit.intent.toAttack
    toGather  = unit.intent.toGather
    toBoard   = unit.intent.toBoard.orElse(toBoard)
    toNuke    = unit.intent.toNuke
    if ( ! combat.tryDodge()) unit.intent.action(unit)
    Debugging.CherryVis.CherryVis.noteBehavior(unit.id, unit.intent.action)
    flushDecisionChain()
  }

  /////////////
  // Pushing //
  /////////////

  def escalatePriority(newPriority: TrafficPriority): Unit = if (priority < newPriority && ! unit.flying) priority = newPriority
  val receivedPushForces = new Cache(() => With.coordinator.pushes
    .get(unit)
    .map(p => (p, p.force(unit)))
    .filter(_._2.exists(_.lengthSquared > 0))
    .map(p => (p._1, p._2.get))
    .toVector)
  val receivedPushForce = new KeyedCache(() => ForceMath.sum(receivedPushForces().view.filter(_._1.priority >= priority).map(_._2)), () => priority)
  val receivedPushPriority = new KeyedCache(() => Maff.max(receivedPushForces().view.map(_._1.priority)).getOrElse(TrafficPriorities.Freedom), () => priority)

  /////////////
  // Leading //
  /////////////

  val leader = new Cache(() => unit.squad.flatMap(_.leader(unit.unitClass)))
  var leadFollower: (FriendlyUnitInfo) => Unit = x => {}

  def isLeader: Boolean = leader().contains(unit)

  /////////////////
  // Ridesharing //
  /////////////////

  private val _passengers : ArrayBuffer[FriendlyUnitInfo] = new ArrayBuffer[FriendlyUnitInfo]
  private var _ride       : Option[FriendlyUnitInfo]      = None
  private var _rideGoal   : Option[Pixel]                 = None

  private def reservedPassengers: Vector[FriendlyUnitInfo] = _passengers.synchronized(_passengers.toVector)

  def passengers            : Seq[FriendlyUnitInfo]     = (reservedPassengers ++ unit.loadedUnits).distinct
  def passengersPrioritized : Seq[FriendlyUnitInfo]     = passengers.sortBy(p => - p.unitClass.subjectiveValue - p.energy / 250.0 + p.frameDiscovered / 1e6)
  def hailers               : Seq[FriendlyUnitInfo]     = passengers            .filter(p => ! p.loaded && p.agent.wantsPickup)
  def hailersPrioritized    : Seq[FriendlyUnitInfo]     = passengersPrioritized .filter(p => ! p.loaded && p.agent.wantsPickup)
  def passengerSize         : Int                       = passengers.view.map(_.unitClass.spaceRequired).sum
  def ride                  : Option[FriendlyUnitInfo]  = _ride
  def rideGoal              : Option[Pixel]             = _rideGoal
  def isPrimaryPassenger    : Boolean                   = ride.exists(_.agent.passengersPrioritized.headOption.contains(unit))

  def addPassenger(passenger: FriendlyUnitInfo): Unit = {
    if (passengerSize + passenger.unitClass.spaceRequired > 8 && ! passenger.transport.contains(unit)) return

    passenger.agent.ride.foreach(_.agent.removePassenger(passenger))
    passenger.agent._ride = Some(unit)
    _passengers.synchronized {
      _passengers -= passenger
      _passengers += passenger
    }
  }
  def removePassenger(passenger: FriendlyUnitInfo): Unit = {
    passenger.agent._ride = passenger.agent._ride.filter(_ != unit)
    _passengers.synchronized {
      _passengers -= passenger
    }
  }
  def removeAllPassengers(): Unit = {
    val all = passengers.toVector
    all.foreach(removePassenger)
  }
  def setRideGoal(to: Pixel): Unit = {
    _rideGoal = Some(to)
  }

  ////////////////
  // Blackboard //
  ////////////////

  val blackboard: mutable.Map[Any, Any] = new mutable.HashMap[Any, Any]

  def put(key: Any, value: Any = "Blank"): Unit = {
    blackboard(key) = value
  }
  def get(key: Any): Option[Any] = {
    blackboard.get(key)
  }
  def get[T](key: Any)(implicit tag: ClassTag[T]): Option[T] = {
    blackboard.get(key).filter(tag.runtimeClass.isInstance).map(_.asInstanceOf[T])
  }
  def contains(key: Any): Boolean = {
    blackboard.contains(key)
  }
  def remove(key: Any): Option[Any] = {
    blackboard.remove(key)
  }

  ///////////
  // Other //
  ///////////

  def isScout: Boolean = unit.intent.toScoutTiles.nonEmpty

  def choosePerch(): Destination = perch.set(perch.pixel.orElse(toAttack.map(unit.pixelToFireAtExhaustive)))

  override def toString: String = f"${lastAction.mkString} ${toAttack.map(t => f"$t ").mkString}${super.toString}"
}

