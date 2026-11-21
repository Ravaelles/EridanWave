
package Micro.Agency

import Lifecycle.With
import Mathematics.Physics.ForceMap
import Mathematics.Points.Pixel
import Micro.Actions.Combat.Decisioning.Combat
import Micro.Coordination.Pushing.{TrafficPriorities, TrafficPriority}
import Performance.Cache
import ProxyBwapi.Techs.Tech
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
  var spellIntent   : Option[(Tech, Pixel, Int)]  = None // (Spell, committed pixel, frame): approach it, reconfirm in range, then cast
  var toAttack      : Option[UnitInfo]          = None
  var toAttackLast  : Option[UnitInfo]          = None
  var toGather      : Option[UnitInfo]          = None
  var toBoard       : Option[FriendlyUnitInfo]  = None
  var creepAnchor   : Option[Pixel]             = None // HoldLine creep: the advancing hold position, cleared when not creeping
  var shouldFight   : Boolean                   = false
  var commit        : Boolean                   = false
  var wantsUnload   : Boolean                   = false
  var wantsPickup   : Boolean                   = false
  /**
   * The single writer for the fight verdict: the value and its reason
   * always travel together, so the trace never shows one without the
   * other. The reason defaults to sticky: verdicts that only flip the
   * value (rush assessment, muta recovery) keep whatever reason the
   * frame already carried, exactly as the direct writes did before.
   */
  def decideFight(value: Boolean, reason: String = fightReason): Unit = {
    shouldFight = value
    fightReason = reason
  }
  val forces        : ForceMap                  = new ForceMap
  val combat        : Combat                    = new Combat(unit)

  /////////////////
  // Diagnostics //
  /////////////////

  var fightReason : String           = ""
  var lastAction  : Option[String]   = None
  val actions     : ArrayBuffer[String] = new ArrayBuffer[String]()

  def act(value: String): Unit = {
    actions += value
    lastAction = Some(value)
  }

  /**
   * Chain containers never credit themselves (Action.isContainer): only
   * leaf behaviors reach this buffer, so the trace shows the reasoning
   * path, not its end. Adding a container means overriding the flag on
   * the action itself, never editing a list here.
   */
  private def flushDecisionChain(): Unit = {
    val chain = actions.distinct
    if (chain.nonEmpty) {
      val label = chain.mkString("/")
      Debugging.CherryVis.CherryVis.decision(unit.id, label)
      Debugging.Probe.note("Act", unit.id, label)
      if (With.configuration.formationAudit) Debugging.FormationAudit.tally(label)
      Debugging.DeathWatch.recordBehavior(unit.id, label)
      Debugging.BehaviorLog.note(unit.id, label)
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

    ridesharing.reconcileBoarding()

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

  val traffic: Traffic = new Traffic(unit)
  def priority: TrafficPriority = traffic.priority
  def priority_=(value: TrafficPriority): Unit = { traffic.priority = value }
  def escalatePriority(newPriority: TrafficPriority): Unit = traffic.escalatePriority(newPriority)
  def receivedPushForces = traffic.receivedPushForces
  def receivedPushForce = traffic.receivedPushForce
  def receivedPushPriority = traffic.receivedPushPriority

  /////////////
  // Leading //
  /////////////

  val leader = new Cache(() => unit.squad.flatMap(_.leader(unit.unitClass)))
  var leadFollower: (FriendlyUnitInfo) => Unit = x => {}

  def isLeader: Boolean = leader().contains(unit)

  /////////////////
  // Ridesharing //
  /////////////////

  val ridesharing: Ridesharing = new Ridesharing(unit)
  def passengers: Seq[FriendlyUnitInfo] = ridesharing.passengers
  def passengersPrioritized: Seq[FriendlyUnitInfo] = ridesharing.passengersPrioritized
  def hailers: Seq[FriendlyUnitInfo] = ridesharing.hailers
  def hailersPrioritized: Seq[FriendlyUnitInfo] = ridesharing.hailersPrioritized
  def passengerSize: Int = ridesharing.passengerSize
  def ride: Option[FriendlyUnitInfo] = ridesharing.ride
  def rideGoal: Option[Pixel] = ridesharing.rideGoal
  def isPrimaryPassenger: Boolean = ridesharing.isPrimaryPassenger
  def addPassenger(passenger: FriendlyUnitInfo): Unit = ridesharing.addPassenger(passenger)
  def removePassenger(passenger: FriendlyUnitInfo): Unit = ridesharing.removePassenger(passenger)
  def removeAllPassengers(): Unit = ridesharing.removeAllPassengers()
  def setRideGoal(to: Pixel): Unit = ridesharing.setRideGoal(to)

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

