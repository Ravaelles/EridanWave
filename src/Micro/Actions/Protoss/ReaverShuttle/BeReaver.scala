package Micro.Actions.Protoss.ReaverShuttle

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Actions.Combat.Decisioning.Skirmish.FightOrFlee
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Reaver half of the pair doctrine (first claim wins, then generic combat
 * duels): unload onto Zerg statics to kill them on the ground, hold and
 * shoot what the scarab can reach, board only under direct threat or for
 * a real trip - never circle. Anything else (kiting, chasing, marching)
 * stays with the generic fighter, which already knows how.
 */
object BeReaver extends Action {

  override def allowed(unit: FriendlyUnitInfo): Boolean = Protoss.Reaver(unit)

  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    if (unit.loaded) {
      unload(unit)

      return
    }

    val context = GroundContext(
      unit       = unit,
      foe        = Maff.minBy(unit.matchups.targets.view.filter(t => ! t.flying && unit.canAttack(t)))(_.pixelDistanceEdge(unit)),
      rideNearby = unit.agent.ride.exists(r => r.pixelDistanceEdge(unit) <= 64.0))
    if (groundClaims.exists(_.apply(context))) return

    // Duelist walk (baseless maps, shuttle-less pairs only): with no contacts
    // the squad march stalls, so a Reaver with no ride at all walks at the
    // nearest known enemy instead of holding a stale HoldPosition until
    // timeout. With a live ride the hull holds for pickup instead - walking
    // off alone while its Shuttle ferries elsewhere is how Reavers die solo.
    if (With.geography.ourBases.isEmpty && ! With.units.ours.exists(u => u.alive && Protoss.Shuttle(u))) {
      duelWalk(unit)
    }
  }

  /** One frame of context for the ground claims: the ride and the nearest reachable foe, measured once. */
  private final case class GroundContext(unit: FriendlyUnitInfo, foe: Option[UnitInfo], rideNearby: Boolean) {
    def ride: Option[FriendlyUnitInfo] = unit.agent.ride
  }

  /** First claim wins, in order; a claim that acts owns the frame. */
  private val groundClaims: Seq[GroundContext => Boolean] = Seq(
    rescueScarab, backOffOutrangedStatic, boardShotless, standShoot, closeStatic, backOffOutrangedInRange, boardRide, boardIdle)

  /** Pressured mid-shot with the ride at hand: the flying Scarab lands on its own, the hull does not need to tank for it. */
  private def rescueScarab(context: GroundContext): Boolean = {
    if ( ! context.rideNearby) return false

    if ( ! ReaverShuttleReboard.rescueScarab(
      loaded          = context.unit.loaded,
      committed       = context.unit.agent.commit,
      hasActiveScarab = context.unit.hasActiveScarab,
      cooldownLeft    = context.unit.cooldownLeft,
      latencyFrames   = With.latency.latencyFrames,
      threatened      = context.unit.matchups.threatsInRange.nonEmpty)) return false

    context.unit.agent.act("BoardRescue")
    Commander.rightClick(context.unit, context.ride.get)

    true
  }

  /** Inside the reach of a static we outrange (an escort melee dragged us in): step back out to our max reach, keep shooting for free. */
  private def backOffOutrangedStatic(context: GroundContext): Boolean = {
    val backOff = context.foe.filter(t => t.unitClass.isStaticDefense && ReaverShuttleStaticAttack.advantageBackoff(
      context.unit.pixelRangeAgainst(t) * Maff.inv32,
      t.pixelRangeAgainst(context.unit) * Maff.inv32,
      context.unit.pixelDistanceEdge(t) * Maff.inv32))
    backOff.foreach(t => {
      context.unit.agent.act("BackOffStatic")
      val gap = context.unit.pixelRangeAgainst(t) + 8.0 - context.unit.pixelDistanceEdge(t)
      context.unit.agent.forceTo(context.unit.pixel.project(t.pixel, -Math.max(0.0, gap)).walkablePixel)
      Commander.move(context.unit)
    })
    backOff.isDefined
  }

  /** Dry under guns with the ride at hand: posing holds nothing, the shot is already spent - board now. */
  private def boardShotless(context: GroundContext): Boolean = {
    if ( ! context.rideNearby || ! shotlessPressured(context.unit)) return false

    context.unit.agent.act("BoardShotless")
    Commander.rightClick(context.unit, context.ride.get)

    true
  }

  /** Anything the scarab can reach dies from where we stand: holding is the whole plan. */
  private def standShoot(context: GroundContext): Boolean = {
    if ( ! context.foe.exists(context.unit.inRangeToAttack)) return false

    context.unit.agent.act("StandShoot")
    Commander.hold(context.unit)

    true
  }

  /** Close a static we can outwalk and outrange, so the next shot lands inside our reach. */
  private def closeStatic(context: GroundContext): Boolean = {
    val close = context.foe.filter(t =>
      t.unitClass.isStaticDefense
      && context.unit.tileDistanceEdge(t) < ReaverShuttleStaticAttack.engageTiles
      && ! outranges(t, context.unit))
    close.foreach(t => {
      context.unit.agent.act("CloseStatic")
      context.unit.agent.forceTo(t.pixel)
      Commander.move(context.unit)
    })
    close.isDefined
  }

  /** Already inside the reach of a static that outranges us: back off to our own max reach instead of trading. */
  private def backOffOutrangedInRange(context: GroundContext): Boolean = {
    val backOff = context.foe.filter(t =>
      t.unitClass.isStaticDefense && outranges(t, context.unit) && context.unit.inRangeToAttack(t))
    backOff.foreach(t => {
      context.unit.agent.act("BackOffStatic")
      val gap = context.unit.pixelRangeAgainst(t) + 32.0 - context.unit.pixelDistanceEdge(t)
      context.unit.agent.forceTo(context.unit.pixel.project(t.pixel, -Math.max(0.0, gap)).walkablePixel)
      Commander.move(context.unit)
    })
    backOff.isDefined
  }

  /** Board for a real trip, or when the position is lost and the ride is the way out. */
  private def boardRide(context: GroundContext): Boolean = {
    if ( ! context.rideNearby) return false

    if ( ! ReaverShuttleReboard.mayReboard(threatDist(context.unit), context.unit.woundPercent()) && ! needsRide(context.unit)) return false

    context.unit.agent.act("BoardRide")
    Commander.rightClick(context.unit, context.ride.get)

    true
  }

  /** Idle pair on a baseless map: the ride came for us, board it even with nowhere urgent to go. */
  private def boardIdle(context: GroundContext): Boolean = {
    val idle = ReaverShuttlePair.rideOf(context.unit).filter(r =>
      r.pixelDistanceEdge(context.unit) <= 64.0 && ReaverShuttlePair.pairIdle(r, context.unit))
    if (idle.isEmpty) return false

    context.unit.agent.act("BoardIdle")
    Commander.rightClick(context.unit, idle.get)

    true
  }

  private def unload(unit: FriendlyUnitInfo): Unit = {
    if (shouldLand(unit)) {
      Commander.requestUnload(unit)
      unit.agent.act("UnloadStatic")
    } else if (unit.agent.fightReason == FightOrFlee.Reave) {
      // Ordered to fight by the mission or the sim verdict: the loop and
      // the DropSafety veto below it handle the landing spot itself.
      Commander.requestUnload(unit)
      unit.agent.act("UnloadOrdered")
    }
  }

  private def duelWalk(unit: FriendlyUnitInfo): Unit = {
    Maff.minBy(With.units.enemy.view.filter(e => e.alive && e.likelyStillThere && ! e.flying && unit.canAttack(e)))(_.pixelDistanceCenter(unit.pixel)).foreach(foe => {
      unit.agent.act("DuelWalk")
      unit.agent.forceTo(foe.pixel)
      Commander.move(unit)
    })
  }

  /** Land on Zerg defense to kill it: close, undamaged on the way in, nothing inside the wound-scaled margin. */
  private def shouldLand(unit: FriendlyUnitInfo): Boolean = {
    val statics = unit.matchups.enemies.filter(e =>
      e.unitClass.isStaticDefense && e.player.isZerg && e.likelyStillThere && unit.tileDistanceEdge(e) < ReaverShuttleStaticAttack.engageTiles)
    statics.nonEmpty &&
      With.framesSince(unit.lastFrameTakingDamage) > 72 &&
      Maff.minBy(unit.matchups.threats)(_.pixelDistanceEdge(unit))
        .forall(t => unit.tileDistanceEdge(t) > ReaverShuttleStaticAttack.marginTiles(unit.woundPercent()))
  }

  /** Never walk onto guns that outrange us: the Reaver or the squad assignment handles those. */
  private def outranges(threat: UnitInfo, unit: FriendlyUnitInfo): Boolean =
    threat.pixelRangeAgainst(unit) > unit.pixelRangeAgainst(threat)

  /** No scarab to spend while pressured: the hull is a target, not a threat. */
  private def shotlessPressured(unit: FriendlyUnitInfo): Boolean =
    unit.scarabs == 0 && unit.matchups.threatsInRange.nonEmpty

  /** Board only for a real trip: walking distance stays on foot. */
  private def needsRide(unit: FriendlyUnitInfo): Boolean =
    ReaverShuttleFerry.needsRide(unit.pixelDistanceCenter(unit.agent.destinationNext()) * Maff.inv32)

  private def threatDist(unit: FriendlyUnitInfo): Double =
    Maff.minBy(unit.matchups.threats.filter(_.canAttack(unit)))(_.pixelDistanceEdge(unit))
      .map(unit.pixelDistanceEdge(_)).getOrElse(Double.PositiveInfinity)
}
