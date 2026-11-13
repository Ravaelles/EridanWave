package Micro.Actions.Protoss.Shuttle

import Debugging.SimpleString
import Mathematics.Maff
import Mathematics.Points.Pixel
import Micro.Actions.Action
import Micro.Actions.Combat.Maneuvering.{AirFleeGeometry, Retreat}
import Micro.Agency.Commander
import Micro.Coordination.Pathing.MicroPathing
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

import scala.collection.mutable.ArrayBuffer

object BeShuttle extends Action {

  override def allowed(unit: FriendlyUnitInfo): Boolean = Protoss.Shuttle(unit)

  // Wound-scaled threat margin: a fresh Shuttle skates what it can outrun,
  // a bleeding one runs early instead of letting Hydralisks close in.
  // Replaces the flat withinSafetyMargin at the retreat gate below.
  private def unsafe(shuttle: FriendlyUnitInfo): Boolean = {
    val matchups = shuttle.matchups
    matchups.threats.nonEmpty && matchups.pixelsEntangled > -ShuttleFleeGeometry.safetyMarginPixels(
      shuttle.woundPercent(),
      shuttle.flying && shuttle.topSpeed > matchups.groupVs.maxAttackerSpeedVsAir)
  }

  trait QuestType extends SimpleString
  object Deliver  extends QuestType
  object Hover    extends QuestType
  object Pickup   extends QuestType

  case class ShuttleQuest(passenger: FriendlyUnitInfo, quest: QuestType, multiplier: Double, baseCost: Double, perform: () => Unit) {
    private val needsLift = passenger.isAny(Protoss.Reaver, Protoss.HighTemplar, Protoss.Archon) && (passenger.proximity < 0.7 || passenger.matchups.pixelsToThreatRange.exists(_ < 320))
    val finalCost: Double = ShuttleQuestCost.finalCost(baseCost, multiplier, needsLift)
  }

  protected def perform(shuttle: FriendlyUnitInfo): Unit = {
    if (ShuttleDitchPassengers.ditch(shuttle)) return
    if (ShuttleAdoptPassenger.adopt(shuttle)) return
    if (fleeFromScourgeRear(shuttle)) return
    if (fleeFromAirThreats(shuttle)) return
    if (makeEmergencyPickup(shuttle)) return
    if (runBestQuest(shuttle)) return

    ShuttlePark(shuttle)
    ShuttleAwait(shuttle)
  }

  /**
   * A loaded Shuttle runs from Scourge to the rear of its cluster before
   * any other flight decision - at the generic three tiles the Scourge are
   * already on it. BeFlier skips transports, so this discipline is the one
   * owner of every Shuttle move. True when it took over movement.
   */
  private def fleeFromScourgeRear(shuttle: FriendlyUnitInfo): Boolean =
    ShuttleScourgeFlee.fleePixel(shuttle).exists(rear => {
      shuttle.agent.act("FleeScourge")
      shuttle.agent.destinationNext.set(rear)
      Commander.move(shuttle)
      true
    })

  /**
   * Enemies that can hit us inside twelve tiles push us straight out
   * (single) or off their centroid (many), margin scaled by wounds.
   * On top of the flat radius a ranged threat whose reach plus the
   * preemptive band (4 tiles fresh, up to 9 dying) touches us turns us
   * around before its pack closes - sitting in the enemy main while
   * Hydralisks walk up is how Shuttles die stupidly. Survival outranks
   * even emergency pickups - a dead Shuttle saves nobody.
   * True when it took over movement.
   */
  private def fleeFromAirThreats(shuttle: FriendlyUnitInfo): Boolean = {
    val preemptPixels = (ShuttleFleeGeometry.baseFleeMarginTiles
      + shuttle.woundPercentBonus(0.0, ShuttleFleeGeometry.woundFleeBonusTiles)) * 32.0
    val attackersNear = shuttle.matchups.threats.filter(t =>
      t.canAttack(shuttle) && (
        t.pixelDistanceEdge(shuttle) <= 32 * AirFleeGeometry.threatRadiusTiles
        || ShuttleFleeGeometry.threatensPreemptively(
          t.pixelDistanceEdge(shuttle),
          t.pixelRangeAgainst(shuttle),
          preemptPixels)))
    AirFleeGeometry.focus(attackersNear.map(_.pixel)).exists(focus => {
      shuttle.agent.decision.set(AirFleeGeometry.fleePixel(shuttle.pixel, focus, shuttle.woundPercent()))
      shuttle.agent.act("AirFlee")
      Commander.move(shuttle)
      true
    })
  }

  /** A passenger that is about to die outranks whatever we were doing. */
  private def makeEmergencyPickup(shuttle: FriendlyUnitInfo): Boolean =
    emergencyHailer(shuttle).exists(hailer => {
      pickup(shuttle, hailer)
      shuttle.agent.act("EmergencyPickup")
      true
    })

  private def emergencyHailer(shuttle: FriendlyUnitInfo): Option[FriendlyUnitInfo] = {
    val supported = shuttle.agent.passengersPrioritized
    val hailers = shuttle.agent.hailersPrioritized.filter(supported.contains)
    val mostUrgent = Maff.orElse(
      hailers.filter(_.doomed),
      hailers.filter(_.likelyDoomed),
      hailers.filter(_.likelyDoomedInFrames < 24),
      hailers.filter(_.matchups.engagedUpon),
      hailers.filter(_.matchups.pixelsToThreatRange.exists(_ < 64)))
    Maff.minBy(mostUrgent)(_.pixelDistanceEdge(shuttle))
  }

  /** Cost the ordinary pickups, dropoffs and parking, and fly the cheapest. */
  private def runBestQuest(shuttle: FriendlyUnitInfo): Boolean = {
    val supported = shuttle.agent.passengersPrioritized
    val passengersFighting = supported.filter(p => isFighting(p))
    val anyoneFighting = passengersFighting.nonEmpty

    val quests = new ArrayBuffer[ShuttleQuest]
    supported.foreach(p => {
      val goal = p.agent.rideGoal
      val multiplier = ShuttleQuestCost.multiplier(passengersFighting.contains(p), anyoneFighting)

      def add(transform: Pixel => ShuttleQuest): Unit = { quests ++= goal.map(transform) }

      lazy val doDeliver = p.loaded
      lazy val doPickup  = goal.exists(g => p.framesToTravelTo(g) > ShuttleQuestCost.pickupPatienceFrames(p.canAttack, p.unitClass.cooldownOnDrop)) && p.agent.toAttack.forall(t => Math.min(p.pixelsToGetInRange(t), p.pixelsToGetInRange(t, t.projectFrames(p.unitClass.cooldownOnDrop))) > 32)
      lazy val doHover   = p.agent.toAttack.isDefined || p.agent.perch.isDefined || p.team.exists(_.engagedUpon) || p.matchups.pixelsToThreatRange.exists(_ < ShuttleQuestCost.fightRadiusPixels)
      if (doDeliver)     { add(g => ShuttleQuest(p, Deliver, multiplier, shuttle.pixelDistanceCenter(g),     () => deliver(shuttle, g)))
      } else if (doPickup) { add(g => ShuttleQuest(p, Pickup, multiplier, shuttle.pixelDistanceEdge(p),       () => pickup(shuttle, p)))
      } else if (doHover)  { add(g => ShuttleQuest(p, Hover,  multiplier, shuttle.pixelDistanceEdge(p) + 160, () => ShuttlePark.park(shuttle, Seq(p.pixel))))
      }
    })

    Maff.minBy(quests)(_.finalCost).exists(quest => {
      quest.perform()
      shuttle.agent.act(quest.quest.toString)
      true
    })
  }

  /** A passenger with something to shoot, a perch, or enemies close by. */
  private def isFighting(p: FriendlyUnitInfo): Boolean = (
    p.agent.toAttack.isDefined
    || p.agent.perch.isDefined
    || p.matchups.pixelsToThreatRange.exists(_ < ShuttleQuestCost.fightRadiusPixels)
    || p.matchups.enemies.exists(e => Protoss.Shuttle(e) && e.pixelDistanceEdge(p) < ShuttleQuestCost.fightRadiusPixels)
  )

  // Pick up a passenger
  //
  private def pickup(shuttle: FriendlyUnitInfo, passenger: FriendlyUnitInfo): Unit = {
    shuttle.agent.decision.set(passenger.pixel)
    if (shuttle.pixelDistanceCenter(passenger) <= 384) {
      Commander.rightClick(shuttle, passenger)
    } else if (unsafe(shuttle)) {
      Retreat.delegate(shuttle)
    } else {
      MicroPathing.tryMovingAlongTilePath(shuttle, MicroPathing.getThreatAwarePath(shuttle, preferHome = false, to = Some(passenger.tile)))
    }
  }

  // Heed a passenger's ride goal
  //
  private def deliver(shuttle: FriendlyUnitInfo, goal: Pixel): Unit = {
    shuttle.agent.decision.set(goal)
    Commander.move(shuttle)
  }
}

