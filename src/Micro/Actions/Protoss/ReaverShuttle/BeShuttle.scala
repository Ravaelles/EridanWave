package Micro.Actions.Protoss.ReaverShuttle

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Actions.Combat.Maneuvering.AirFlee
import Micro.Actions.Combat.Maneuvering.CornerAntiStuckGuard
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
  override def isContainer: Boolean = true

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
      shuttle.agent.forceTo(passenger.pixel)
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
      // only hailing, endangered, travelling or Scarab-targeted Reavers -
      // and never one already fighting something hittable unless it is
      // explicitly hailing: a fighting hull holds its ground instead of
      // boarding, so the ride escorts (hovering boardable) instead of
      // fetching past it forever.
      && (passenger.matchups.targetedByScarab
        || passenger.agent.wantsPickup
        || (Protoss.Reaver(passenger)
          && passenger.matchups.targetsInRange.isEmpty
          && (endangered(shuttle, passenger) || travelsFar(passenger) || ReaverShuttlePair.pairIdle(shuttle, passenger) || strandedFetch(shuttle, passenger) || shuttle.agent.passengers.contains(passenger)))
        || (Protoss.HighTemplar(passenger) && MacroFacts.techStarted(Protoss.PsionicStorm)))
      && (shuttle.agent.passengers.isEmpty || shuttle.agent.passengers.headOption.map(_.squad).getOrElse(shuttle.squad).forall(passenger.squad.contains))
    )

  /**
   * Idle hull, stranded Reaver: a Shuttle standing still by the main while
   * a Reaver sits within twenty tiles and no ground enemy is inside
   * eleven goes and picks it up. The empty hull otherwise awaited the
   * army while the Reaver walked into the same choke all game. Once the
   * fetch is committed the reservation keeps the hail alive as the hull
   * moves off its idle spot.
   */
  private def strandedFetch(shuttle: FriendlyUnitInfo, passenger: FriendlyUnitInfo): Boolean = {
    if (With.geography.ourBases.isEmpty) return false

    FerryFetch.shouldFetch(
      shuttleToMainTiles = shuttle.pixelDistanceCenter(With.geography.ourMain.heart.center) * Maff.inv32,
      stationary         = shuttle.speed < FerryFetch.stationarySpeed,
      reaverDistTiles  = shuttle.pixelDistanceEdge(passenger) * Maff.inv32,
      groundFoeTiles     = Maff.minBy(
        With.units.enemy.view.filter(e => ! e.flying && e.likelyStillThere))(_.pixelDistanceEdge(shuttle))
        .map(_.pixelDistanceEdge(shuttle) * Maff.inv32)
        .getOrElse(Double.PositiveInfinity))
  }

  private def endangered(shuttle: FriendlyUnitInfo, hailer: FriendlyUnitInfo): Boolean =
    hailer.matchups.framesOfSafety < shuttle.framesToTravelTo(hailer.pixel) + 2 * shuttle.unitClass.framesToTurn180

  private def sojourning(hailer: FriendlyUnitInfo): Boolean =
    hailer.agent.destinationFinal().pixelDistance(hailer.pixel) > 32.0 * 20

  /**
   * Travelling far: the same trip length that makes the Reaver board
   * (ReaverShuttleFerry.needsRide) makes the Shuttle come and fetch it.
   * Without the shared definition the pair deadlocks one tile past it:
   * the ride waits for a hail the hull only raises at boarding range,
   * and the hull walks into guns it meant to fly over.
   */
  private def travelsFar(passenger: FriendlyUnitInfo): Boolean =
    ReaverShuttleFerry.needsRide(passenger.pixelDistanceCenter(passenger.agent.destinationNext()) * Maff.inv32)

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
      shuttle.agent.forceTo(CornerAntiStuckGuard.guard(
        rear,
        With.mapPixelWidth,
        With.mapPixelHeight,
        CornerAntiStuckGuard.shuttleTiles))
      Commander.move(shuttle)

      return true
    }

    // Bait rides flee shorter when hurt (they are the provocation, not the
    // cargo): a wound-scaled 4-to-9 band instead of the pair's 4-to-13.
    val preemptPixels = (if (ReaverShuttlePair.isBaitRide(shuttle)) shuttle.woundPercentBonus(4.0, 9.0)
      else ReaverShuttleFlight.baseFleeMarginTiles + shuttle.woundPercentBonus(0.0, ReaverShuttleFlight.woundFleeBonusTiles)) * 32.0
    // Latched on the worst penetration across every attacker: a sharp
    // per-threat edge flips each time the pack breathes, so the ferry
    // never delivers. Melee that cannot reach still counts nothing, and
    // the flee runs from the worst threat, not the nearest pixel.
    val attackers = shuttle.matchups.threats.view.filter(t => t.canAttack(shuttle) && t.likelyStillThere).toVector
    val scored = attackers.view.flatMap(t =>
      if (Zerg.Scourge(t))
        Some((ReaverShuttleFlight.penetrationPixels(t.pixelDistanceEdge(shuttle), ReaverShuttleFlight.scourgeDistTiles * 32.0), t.pixel))
      else {
        val reach = t.pixelRangeAgainst(shuttle)
        if (Micro.Actions.Combat.Maneuvering.RangedReach.isRanged(reach))
          Some((ReaverShuttleFlight.penetrationPixels(t.pixelDistanceEdge(shuttle), reach + preemptPixels), t.pixel))
        else None
      }).toVector
    val worstValue = ReaverShuttleFlight.worstPenetration(scored.map(_._1))
    if ( ! ReaverShuttleFlight.fleeLatched(shuttle.id, worstValue)) return false

    val focus = scored.reduceOption((a, b) => if (a._1 >= b._1) a else b).map(_._2).getOrElse(shuttle.pixel)
    shuttle.agent.act("AirFlee")
    // Corner-guarded on the Shuttle width: a 4->13-tile flee leg ending
    // in a corner trap strands the hull until something kills it, so a
    // destination inside the 15x15 squares slides along the nearest edge.
    val away = CornerAntiStuckGuard.guard(
      shuttle.pixel.project(focus, -preemptPixels).walkablePixel,
      With.mapPixelWidth,
      With.mapPixelHeight,
      CornerAntiStuckGuard.shuttleTiles)
    shuttle.agent.forceTo(away)
    Commander.move(shuttle)

    true
  }

  /** Raid status with a loaded hull: fly the pair at the enemy base. */
  private def raid(shuttle: FriendlyUnitInfo): Boolean = {
    if (shuttle.loadedUnitCount == 0) return false

    val goal = ReaverShuttlePair.raidGoal(shuttle)
    shuttle.agent.act("RaidFerry")
    shuttle.agent.forceTo(goal)
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
      shuttle.agent.forceTo(ReaverShuttlePair.armyGoal)
      Commander.move(shuttle)
    }
  }

  /** Bait: no pair cargo exists anywhere, so the empty hull flies at the
    * enemy main and pulls combat units out of position instead of hovering
    * over our own army. */
  private def bait(shuttle: FriendlyUnitInfo): Unit = {
    shuttle.agent.act("BaitRide")
    shuttle.agent.forceTo(ReaverShuttlePair.baitGoal)
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
    shuttle.agent.forceTo(standoff(shuttle, centroid))
    Commander.move(shuttle)
  }

  /**
   * Escort the unloaded mate: after a static-attack unload the bond is
   * no bond in code (passengerOf reads loaded hulls only), so the empty
   * ride awaited at the army goal while its Reaver died alone. Hover the
   * fighting mate at board reach instead, off the threat - the rescue
   * and reboard below it assume the ride inside 64 pixels. The hold is
   * latched in the pair (escortLatched): the escort/await choice flapped
   * every other frame, so once the ride commits to its mate it holds
   * until the mate is gone or walks out of board reach plus a margin.
   */
  private def escort(shuttle: FriendlyUnitInfo): Boolean = {
    ReaverShuttlePair.mateOnFoot(shuttle)
      .filter(mate => ReaverShuttlePair.escortLatched(
        shuttle.id,
        mate.pixelDistanceCenter(shuttle.pixel) * Maff.inv32))
      .map(mate => {
        shuttle.agent.act("EscortRide")
        shuttle.agent.forceTo(standoff(shuttle, mate.pixel))
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
      shuttle.agent.forceTo(target)
      Commander.move(shuttle)
    }).getOrElse(park(shuttle))
  }
}
