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
    // A Shuttle already carrying a Reaver belongs to the pair: never fetch
    // other passengers for it (no travel/assemble for other units' sake).
    if (shuttle.loadedUnits.exists(Protoss.Reaver(_))) return false

    val pickupCandidates =
      Maff.orElse(
        shuttle.alliesSquad.view.filter(shouldAdopt(shuttle, _)),
        shuttle.alliesBattle.view.filter(shouldAdopt(shuttle, _)),
        With.units.ours.filter(shouldAdopt(shuttle, _)))

    var acted = false
    // Fetch the neediest unboarded hailer; adoption alone strands it.
    Maff.maxBy(pickupCandidates.filter(_.complete))(
      c => pickupNeed(shuttle, c) / (1.0 + c.pixelDistanceSquared(shuttle))).foreach(passenger => {
      shuttle.agent.addPassenger(passenger)
      shuttle.agent.act("FetchPassenger")
      shuttle.agent.destinationNext.set(passenger.pixel)
      Commander.move(shuttle)
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
      // A healthy Reaver holding its ground is not a pickup: adopting it
      // glues the ride to a hull that will never board, and the chase
      // claims every frame below, starving escort, park and await. Fetch
      // only hailing, endangered, travelling or Scarab-targeted Reavers.
      && (passenger.matchups.targetedByScarab
        || passenger.agent.wantsPickup
        || (Protoss.Reaver(passenger) && (endangered(shuttle, passenger) || sojourning(passenger)))
        || (Protoss.HighTemplar(passenger) && MacroFacts.techStarted(Protoss.PsionicStorm)))
      && (shuttle.agent.passengers.isEmpty || shuttle.agent.passengers.headOption.map(_.squad).getOrElse(shuttle.squad).forall(passenger.squad.contains))
    )

  private def endangered(shuttle: FriendlyUnitInfo, hailer: FriendlyUnitInfo): Boolean =
    hailer.matchups.framesOfSafety < shuttle.framesToTravelTo(hailer.pixel) + 2 * shuttle.unitClass.framesToTurn180

  private def sojourning(hailer: FriendlyUnitInfo): Boolean =
    hailer.agent.destinationFinal().pixelDistance(hailer.pixel) > 32.0 * 20

  private def pickupNeed(shuttle: FriendlyUnitInfo, hailer: FriendlyUnitInfo): Double = {
    var output = 1.0
    if (Protoss.Reaver(hailer))                     output *= 1000
    if (hailer.matchups.targetedByScarab)           output *= 100
    if (shuttle.squad.exists(hailer.squad.contains)) output *= 10
    if (shuttle.battle.exists(hailer.battle.contains)) output *= 5
    if (endangered(shuttle, hailer))                output *= 10
    if (sojourning(hailer))                         output *= 2
    output
  }

  /** Run from Scourge to the rear, or from anything that can hit the hull. */
  private def flee(shuttle: FriendlyUnitInfo): Boolean = {
    val scourgeRear = shuttle.matchups.threats.exists(t =>
      Zerg.Scourge(t) && t.alive && t.pixelDistanceEdge(shuttle) < ReaverShuttleFlight.scourgeDistTiles * 32.0)
    if (scourgeRear) {
      val members = shuttle.matchups.allies.view.filter(a => a.flying && a != shuttle && a.pixelDistanceCenter(shuttle) < 20 * 32).map(_.pixel).toSeq :+ shuttle.pixel
      // Baseless maps (UMS duels) have no main: fall back to map home.
      val home = if (With.geography.ourBases.isEmpty) With.geography.home.center else With.geography.ourMain.heart.center
      val rear = ReaverShuttleFlight.rearOfCluster(members, home)
      shuttle.agent.act("FleeScourge")
      shuttle.agent.destinationNext.set(rear)
      Commander.move(shuttle)

      return true
    }

    // Bait rides flee shorter when hurt (they are the provocation, not the
    // cargo): a wound-scaled 4-to-9 band instead of the pair's 4-to-13.
    val preemptPixels = (if (ReaverShuttlePair.isBaitRide(shuttle)) shuttle.woundPercentBonus(4.0, 9.0)
      else ReaverShuttleFlight.baseFleeMarginTiles + shuttle.woundPercentBonus(0.0, ReaverShuttleFlight.woundFleeBonusTiles)) * 32.0
    val attackers = shuttle.matchups.threats.filter(t =>
      t.canAttack(shuttle) && t.likelyStillThere && (
        (Zerg.Scourge(t) && t.pixelDistanceEdge(shuttle) <= ReaverShuttleFlight.scourgeDistTiles * 32.0) ||
          ReaverShuttleFlight.threatensPreemptively(t.pixelDistanceEdge(shuttle), t.pixelRangeAgainst(shuttle), preemptPixels)))
    Maff.minBy(attackers.map(_.pixel))(_.pixelDistanceSquared(shuttle.pixel)).map(focus => {
      shuttle.agent.act("AirFlee")
      shuttle.agent.destinationNext.set(shuttle.pixel.project(focus, -preemptPixels).walkablePixel)
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

  /** Otherwise stick with the army: park with passengers, escort the unloaded mate, await the next one. */
  private def support(shuttle: FriendlyUnitInfo): Unit = {
    if (ReaverShuttlePair.passengerOf(shuttle).isDefined) {
      if (With.geography.ourBases.isEmpty) duel(shuttle) else park(shuttle)
    } else if (escort(shuttle)) {
    } else if (ReaverShuttlePair.isBaitRide(shuttle)) {
      bait(shuttle)
    } else {
      shuttle.agent.act("AwaitRide")
      shuttle.agent.destinationNext.set(ReaverShuttlePair.armyGoal)
      Commander.move(shuttle)
    }
  }

  /** Bait: no pair cargo exists anywhere, so the empty hull flies at the
    * enemy main and pulls combat units out of position instead of hovering
    * over our own army. */
  private def bait(shuttle: FriendlyUnitInfo): Unit = {
    shuttle.agent.act("BaitRide")
    shuttle.agent.destinationNext.set(ReaverShuttlePair.baitGoal)
    Commander.move(shuttle)
  }

  /** Hover over the live cargo near its centroid, off the threat. Only the
    * loaded hulls count: unboarded hailers (stale reservations included)
    * drag the centroid to places the cargo never goes - a shuttle has sat
    * out a fight in its natural this way instead of hovering its Reaver. */
  private def park(shuttle: FriendlyUnitInfo): Unit = {
    val cargo = shuttle.loadedUnits.view.filterNot(_.airborne)
    val centroid = Maff.centroid(Maff.orElse(cargo.map(_.pixel), Seq(shuttle.pixel)))
    shuttle.agent.act("ParkRide")
    shuttle.agent.destinationNext.set(standoff(shuttle, centroid))
    Commander.move(shuttle)
  }

  /**
   * Escort the unloaded mate: after a static-attack unload the bond is
   * no bond in code (passengerOf reads loaded hulls only), so the empty
   * ride awaited at the army goal while its Reaver died alone. Hover the
   * fighting mate at board reach instead, off the threat - the rescue
   * and reboard below it assume the ride inside 64 pixels.
   */
  private def escort(shuttle: FriendlyUnitInfo): Boolean = {
    ReaverShuttlePair.mateOnFoot(shuttle)
      .filter(mate => ReaverShuttlePair.shouldEscort(mate.pixelDistanceCenter(shuttle.pixel) * Maff.inv32))
      .map(mate => {
        shuttle.agent.act("EscortRide")
        shuttle.agent.destinationNext.set(standoff(shuttle, mate.pixel))
        Commander.move(shuttle)
        true
      }).getOrElse(false)
  }

  /** Board-reach hover off the threat: stand off past pickup reach so the ride stays boardable under fire. */
  private def standoff(shuttle: FriendlyUnitInfo, point: Mathematics.Points.Pixel): Mathematics.Points.Pixel = {
    val threat = Maff.minBy(shuttle.matchups.threats)(t => t.pixelDistanceCenter(point) - t.pixelRangeAgainst(shuttle))
    threat
      .map(t => t.pixel.project(point, t.pixelDistanceCenter(point) + 64.0).walkablePixel)
      .getOrElse(point)
  }

  /**
   * Duelist ferry (baseless maps only): no bases means no mission and no
   * army to support, so the loaded pair flies at the nearest known enemy
   * instead of parking in place - the static-attack unload fires the
   * landing, flee answers the danger. With nothing known, park.
   */
  private def duel(shuttle: FriendlyUnitInfo): Unit = {
    ReaverShuttlePair.duelTarget(shuttle.pixel).map(target => {
      shuttle.agent.act("DuelFerry")
      shuttle.agent.destinationNext.set(target)
      Commander.move(shuttle)
    }).getOrElse(park(shuttle))
  }
}
