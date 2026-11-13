package Micro.Actions.Protoss.ReaverShuttle

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Agency.Commander
import Macro.Facts.MacroFacts
import ProxyBwapi.Races.{Protoss, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.attack.SquadAttack
import Tactic.Squads.defense.SquadDefendBase

/**
 * Shuttle half of the pair doctrine (first claim wins): ditch
 * hitchhikers, adopt the neediest hailer, run from what can hit the
 * hull, raid the enemy base on a RaidBase status, otherwise stick with
 * the army - park with passengers, await the next Reaver when empty.
 */
object BeShuttle extends Action {

  override def allowed(unit: FriendlyUnitInfo): Boolean = Protoss.Shuttle(unit)

  override protected def perform(shuttle: FriendlyUnitInfo): Unit = {
    if (ditch(shuttle)) return
    if (adopt(shuttle)) return
    if (flee(shuttle)) return

    val status = ReaverShuttlePair.statusOfShuttle(shuttle)
    if (status == ReaverShuttleStatus.RaidBase) {
      if (raid(shuttle)) return
    }
    support(shuttle)
  }

  /** Drop passengers that are not ours to ferry. */
  private def ditch(shuttle: FriendlyUnitInfo): Boolean = {
    if (shuttle.base.exists(_.owner.isEnemy) && ! shuttle.agent.destinationFinal().base.exists(_.owner.isEnemy)) return false
    val hitchhikers = (shuttle.agent.passengers ++ shuttle.loadedUnits)
      .distinct
      .filter(p => p.isNone(Protoss.Reaver, Protoss.HighTemplar) && ! p.squad.exists(shuttle.squad.contains))
      .toVector
    if (hitchhikers.isEmpty) return false

    hitchhikers.foreach(shuttle.agent.removePassenger)
    hitchhikers.foreach(Commander.unload(shuttle, _))
    shuttle.agent.act("ShuttleDitch")

    true
  }

  /** Pick up the neediest hailer, plus newborn Reavers for an army squad. */
  private def adopt(shuttle: FriendlyUnitInfo): Boolean = {
    val pickupCandidates =
      Maff.orElse(
        shuttle.alliesSquad.view.filter(shouldAdopt(shuttle, _)),
        shuttle.alliesBattle.view.filter(shouldAdopt(shuttle, _)),
        With.units.ours.filter(shouldAdopt(shuttle, _)))

    var acted = false
    Maff.maxBy(pickupCandidates)(c => pickupNeed(shuttle, c) / (1.0 + c.pixelDistanceSquared(shuttle))).foreach(passenger => {
      shuttle.agent.addPassenger(passenger)
      acted = true
    })

    if (shuttle.squad.exists(s => s.isInstanceOf[SquadAttack] || s.isInstanceOf[SquadDefendBase])) {
      val newborns = With.units.ours
        .filter(u => ! u.complete && Protoss.Reaver(u) && u.remainingCompletionFrames < shuttle.framesToTravelTo(u.pixel))
        .toVector
      Maff.minBy(newborns)(_.remainingCompletionFrames).foreach(newborn => {
        shuttle.agent.addPassenger(newborn)
        acted = true
      })
    }
    if (acted) shuttle.agent.act("ShuttleAdopt")

    acted
  }

  private def shouldAdopt(shuttle: FriendlyUnitInfo, passenger: FriendlyUnitInfo): Boolean = (
    shuttle.canTransport(passenger)
      && shuttle.squad.forall {
        case drop: Tactic.Missions.core.MissionDrop => drop.assignedTransport(passenger).contains(shuttle)
        case _ => true
      }
      && passenger.transport.isEmpty
      && passenger.agent.ride.forall(shuttle==)
      && passenger.unitClass.spaceRequired + shuttle.agent.passengerSize <= shuttle.unitClass.spaceProvided
      && (passenger.matchups.targetedByScarab || Protoss.Reaver(passenger) || (Protoss.HighTemplar(passenger) && MacroFacts.techStarted(Protoss.PsionicStorm)))
      && (shuttle.agent.passengers.isEmpty || shuttle.agent.passengers.headOption.map(_.squad).getOrElse(shuttle.squad).forall(passenger.squad.contains))
    )

  private def pickupNeed(shuttle: FriendlyUnitInfo, hailer: FriendlyUnitInfo): Double = {
    val endangered = hailer.matchups.framesOfSafety < shuttle.framesToTravelTo(hailer.pixel) + 2 * shuttle.unitClass.framesToTurn180
    val sojourning = hailer.agent.destinationFinal().pixelDistance(hailer.pixel) > 32.0 * 20
    var output = 1.0
    if (Protoss.Reaver(hailer))                     output *= 1000
    if (hailer.matchups.targetedByScarab)           output *= 100
    if (shuttle.squad.exists(hailer.squad.contains)) output *= 10
    if (shuttle.battle.exists(hailer.battle.contains)) output *= 5
    if (endangered)                                 output *= 10
    if (sojourning)                                 output *= 2
    output
  }

  /** Run from Scourge to the rear, or from anything that can hit the hull. */
  private def flee(shuttle: FriendlyUnitInfo): Boolean = {
    val scourgeRear = shuttle.matchups.threats.exists(t =>
      Zerg.Scourge(t) && t.alive && t.pixelDistanceEdge(shuttle) < ReaverShuttleFlight.scourgeDistTiles * 32.0)
    if (scourgeRear) {
      val members = shuttle.matchups.allies.view.filter(a => a.flying && a != shuttle && a.pixelDistanceCenter(shuttle) < 20 * 32).map(_.pixel).toSeq :+ shuttle.pixel
      val rear = ReaverShuttleFlight.rearOfCluster(members :+ shuttle.pixel, With.geography.ourMain.heart.center)
      shuttle.agent.act("FleeScourge")
      shuttle.agent.destinationNext.set(rear)
      Commander.move(shuttle)

      return true
    }
    val preemptPixels = (ReaverShuttleFlight.baseFleeMarginTiles + shuttle.woundPercentBonus(0.0, ReaverShuttleFlight.woundFleeBonusTiles)) * 32.0
    val attackers = shuttle.matchups.threats.filter(t =>
      t.canAttack(shuttle) && t.likelyStillThere && (
        t.pixelDistanceEdge(shuttle) <= ReaverShuttleFlight.scourgeDistTiles * 32.0 ||
          ReaverShuttleFlight.threatensPreemptively(t.pixelDistanceEdge(shuttle), t.pixelRangeAgainst(shuttle), preemptPixels)))
    Maff.minBy(attackers.map(_.pixel))(_.pixelDistanceSquared(shuttle.pixel)).map(focus => {
      shuttle.agent.act("AirFlee")
      shuttle.agent.destinationNext.set(shuttle.pixel.project(focus, -(preemptPixels + shuttle.pixelDistanceEdge(focus))).walkablePixel)
      Commander.move(shuttle)
      true
    }).getOrElse(false)
  }

  /** Raid status with a loaded hull: fly the pair at the enemy base. */
  private def raid(shuttle: FriendlyUnitInfo): Boolean = {
    if (shuttle.loadedUnitCount == 0) return false

    val goal = ReaverShuttlePair.raidGoal(shuttle)
    shuttle.agent.act("RaidFerry")
    shuttle.agent.destinationNext.set(goal)
    Commander.move(shuttle)

    true
  }

  /** Otherwise stick with the army: park with passengers, await the next one. */
  private def support(shuttle: FriendlyUnitInfo): Unit = {
    if (ReaverShuttlePair.passengerOf(shuttle).isDefined) {
      park(shuttle)
    } else {
      shuttle.agent.act("AwaitRide")
      shuttle.agent.destinationNext.set(ReaverShuttlePair.armyGoal)
      Commander.move(shuttle)
    }
  }

  /** Hover with grounded passengers near their centroid, off the threat. */
  private def park(shuttle: FriendlyUnitInfo): Unit = {
    val exposed = shuttle.agent.passengersPrioritized.filterNot(_.airborne)
    val centroid = Maff.centroid(Maff.orElse(exposed.map(_.pixel), Seq(shuttle.pixel)))
    val threat = Maff.minBy(shuttle.matchups.threats)(t => t.pixelDistanceCenter(centroid) - t.pixelRangeAgainst(shuttle))
    // Stand off past pickup reach so the ride stays boardable under fire.
    val to = threat
      .map(t => t.pixel.project(centroid, t.pixelDistanceCenter(centroid) + 64.0).walkablePixel)
      .getOrElse(centroid)
    shuttle.agent.act("ParkRide")
    shuttle.agent.destinationNext.set(to)
    Commander.move(shuttle)
  }
}
