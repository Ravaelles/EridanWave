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
import Utilities.?

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
    private val passengerScore = ?(
      passenger.isAny(Protoss.Reaver, Protoss.HighTemplar, Protoss.Archon) && (passenger.proximity < 0.7 || passenger.matchups.pixelsToThreatRange.exists(_ < 320)),
      0.0001,
      1.0)
    val finalCost: Double = baseCost * multiplier * passengerScore
  }

  protected def perform(shuttle: FriendlyUnitInfo): Unit = {
    if (ShuttleDitchPassengers(shuttle)) return
    if (ShuttleAdoptPassenger(shuttle)) return

    // Air flee: enemies that can hit us inside twelve tiles push us straight
    // out (single) or off their centroid (many), margin scaled by wounds.
    // Survival outranks even emergency pickups - a dead Shuttle saves nobody.
    val attackersNear = shuttle.matchups.threats.filter(t =>
      t.canAttack(shuttle) && t.pixelDistanceEdge(shuttle) <= 32 * AirFleeGeometry.threatRadiusTiles)
    AirFleeGeometry.focus(attackersNear.map(_.pixel)).foreach(focus => {
      shuttle.agent.decision.set(AirFleeGeometry.fleePixel(shuttle.pixel, focus, shuttle.woundPercent()))
      shuttle.agent.act("AirFlee")
      Commander.move(shuttle)

      return
    })

    // Emergency pickups
    val supported = shuttle.agent.passengersPrioritized
    val hailers = shuttle.agent.hailersPrioritized.filter(supported.contains)
    val hailersMostUrgent = Maff.orElse(
      hailers.filter(_.doomed),
      hailers.filter(_.likelyDoomed),
      hailers.filter(_.likelyDoomedInFrames < 24),
      hailers.filter(_.matchups.engagedUpon),
      hailers.filter(_.matchups.pixelsToThreatRange.exists(_ < 64)))
    val emergencyHailer = Maff.minBy(hailersMostUrgent)(_.pixelDistanceEdge(shuttle))
    emergencyHailer.foreach(u => {
      pickup(shuttle, u)
      shuttle.agent.act("EmergencyPickup")

      return
    })

    // Ordinary pickups/dropoffs/parking
    val fightRadius = 320
    val passengersFighting = supported.filter(p =>
      p.agent.toAttack.isDefined
      || p.agent.perch.isDefined
      || p.matchups.pixelsToThreatRange.exists(_ < fightRadius)
      || p.matchups.enemies.exists(e => Protoss.Shuttle(e) && e.pixelDistanceEdge(p) < fightRadius))
    val anyoneFighting = passengersFighting.nonEmpty

    val quests = new ArrayBuffer[ShuttleQuest]
    supported.foreach(p => {
      val goal = p.agent.rideGoal
      val m = ?(passengersFighting.contains(p) || ! anyoneFighting, 1, 10)

      def add(transform: Pixel => ShuttleQuest): Unit = { quests ++= goal.map(transform) }

      lazy val doDeliver  = p.loaded
      lazy val doPickup   = goal.exists(g => p.framesToTravelTo(g) > 24 + ?(p.canAttack, p.unitClass.cooldownOnDrop, 0)) && p.agent.toAttack.forall(t => Math.min(p.pixelsToGetInRange(t), p.pixelsToGetInRange(t, t.projectFrames(p.unitClass.cooldownOnDrop))) > 32)
      lazy val doHover    = p.agent.toAttack.isDefined || p.agent.perch.isDefined || p.team.exists(_.engagedUpon) || p.matchups.pixelsToThreatRange.exists(_ < 320)
              if (doDeliver)  { add(g => ShuttleQuest(p, Deliver, m, shuttle.pixelDistanceCenter(g),      () => deliver(shuttle, g)))
      } else  if (doPickup)   { add(g => ShuttleQuest(p, Pickup,  m, shuttle.pixelDistanceEdge(p),        () => pickup(shuttle, p)))
      } else  if (doHover)    { add(g => ShuttleQuest(p, Hover,   m, shuttle.pixelDistanceEdge(p) + 160,  () => ShuttlePark.park(shuttle, Seq(p.pixel))))
      }
    })

    val quest = Maff.minBy(quests)(_.finalCost)
    quest.foreach(q => {
      q.perform()
      shuttle.agent.act(q.quest.toString)

      return
    })

    ShuttlePark(shuttle)
    ShuttleAwait(shuttle)
  }

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

