package Micro.Actions.Protoss.ReaverShuttle

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Actions.Combat.Decisioning.Skirmish.FightOrFlee
import Micro.Actions.Combat.Maneuvering.ReaverTankKeepOff
import Micro.Agency.Commander
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Reaver half of the pair doctrine: the arbiter picks one order per
 * frame, this brain executes it. Boarding, fighting, holding, walking
 * and landing are families now, never negotiating claims - the order
 * gates which family runs, each claim keeps its own trigger. Anything
 * else (kiting, chasing, marching) stays with the generic fighter,
 * which already knows how.
 */
object BeReaver extends Action {
  override def isContainer: Boolean = true

  override def allowed(unit: FriendlyUnitInfo): Boolean = Protoss.Reaver(unit)

  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    if (unit.loaded) {
      // Landing is an order: the arbiter opens the attempt on the band
      // or the mission word, unload() refines it the way it always did.
      if (PairArbiter.reaverOrder(unit) == PairArbiter.UnloadHere) unload(unit)

      return
    }

    // Survival first, always: a hull inside a static's reach steps
    // out before any order moves or shoots it deeper in. WalkArmy
    // marched a fresh hull straight into a Sunken without firing.
    if (staticKeepOff(unit)) return

    // Arbiter owns the what, claims own the how: the order gates which
    // family runs, so boarding and fighting never negotiate mid-frame.
    // A quiet crowded hull (ReaverBoardGate vocabulary, now an arbiter
    // fact) fights instead of freezing, and a walking hull shoots its
    // way to the ride instead of marching past the guns.
    val order = PairArbiter.reaverOrder(unit)
    val context = GroundContext(
      unit       = unit,
      foe        = Maff.minBy(unit.matchups.targets.view.filter(t => ! t.flying && unit.canAttack(t)))(_.pixelDistanceEdge(unit)),
      rideNearby = unit.agent.ride.exists(r => r.pixelDistanceEdge(unit) <= 64.0))
    val acted = order match {
      case PairArbiter.RideAlong  => true
      case PairArbiter.BoardRide  => boardingClaims.exists(_.apply(context)) || walkToRide(unit)
      case PairArbiter.FightOnFoot => combatClaims.exists(_.apply(context))
      case PairArbiter.HoldShoot   =>
        if (standShoot(context)) true
        else {
          unit.agent.act("PairShoot")
          Commander.hold(unit)

          true
        }
      case PairArbiter.WalkToRide => combatClaims.exists(_.apply(context)) || walkToRide(unit)
      case PairArbiter.WalkToArmy => combatClaims.exists(_.apply(context)) || walkToArmy(unit)
    }
    if (acted) return

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

  /** Boarding family, in order; the arbiter gates it behind BoardRide. */
  private val boardingClaims: Seq[GroundContext => Boolean] = Seq(
    rescueScarab, boardShotless, backOffSiegedTanks, boardRide, boardIdle)

  /** Combat family, in order; the arbiter gates it behind the fight. */
  private val combatClaims: Seq[GroundContext => Boolean] = Seq(
    backOffOutrangedStatic, standShoot, closeStatic, backOffOutrangedInRange)

  /** Walk to the ride instead of waiting for it: the anti-paralysis leg. */
  private def walkToRide(unit: FriendlyUnitInfo): Boolean = {
    unit.agent.ride.orElse(ReaverShuttlePair.rideOf(unit)) match {
      case Some(ride) =>
        unit.agent.act("PairWalkRide")
        unit.agent.forceTo(ride.pixel)
        Commander.move(unit)

        true
      case None => false
    }
  }

  /** No ride anywhere: walk to the army, never hold for a pickup that is not coming. */
  private def walkToArmy(unit: FriendlyUnitInfo): Boolean = {
    unit.agent.act("PairWalkArmy")
    unit.agent.forceTo(ReaverShuttlePair.armyGoal)
    Commander.move(unit)

    true
  }

  /**
   * Static keep-off (always, every on-foot order): the nearest defensive
   * static that can hit the hull inside the floor costs one step out,
   * away from its pixel, onto a walkable tile. One step per frame keeps
   * a hull sliding along the band instead of diving in or routing out.
   */
  private def staticKeepOff(unit: FriendlyUnitInfo): Boolean = {
    Maff.minBy(unit.matchups.enemies.view.filter(e =>
      e.unitClass.isStaticDefense && e.likelyStillThere && e.canAttack(unit)))(unit.pixelDistanceEdge(_))
      .filter(t => ReaverShuttleStaticAttack.shouldStepOut(unit.tileDistanceEdge(t)))
      .exists(t => {
        unit.agent.act("StaticKeepOff")
        unit.agent.forceTo(unit.pixel.project(t.pixel, -32.0 * ReaverShuttleStaticAttack.stepTiles).walkablePixel)
        Commander.move(unit)

        true
      })
  }

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

  /**
   * Siege line, after the rescue boardings and the outranged-static
   * step-back, before every shooting hold or close: a Reaver never
   * closes inside 12.5 tiles of a sieged tank, even to shoot - the hull
   * only hunts other units. Boarding rescues above keep their precedence
   * (an escaping hull boards instead of walking); with no siege on the
   * map this claim never fires, so the order below is untouched.
   */
  private def backOffSiegedTanks(context: GroundContext): Boolean =
    ReaverTankKeepOff.act(context.unit)

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
    // Rejoin reach, not pickup radius: a pressured hull walks to a ride
    // hovering just past the engine pickup radius instead of standing
    // under fire while it breathes at the edge. Rescue and shotless
    // boarding keep the immediate radius; this is the deliberate reboard.
    val ride = context.ride.getOrElse(return false)
    if ( ! ReaverShuttleReboard.rideInReach(context.unit.pixelDistanceEdge(ride) * Maff.inv32)) return false

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
    if (PairArbiter.landingReady(unit)) {
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
    // Siege tanks are never the walk: the hull only hunts other units,
    // the keep-off above holds the line when the road leads past guns.
    Maff.minBy(With.units.enemy.view.filter(e =>
      e.alive && e.likelyStillThere && ! e.flying && unit.canAttack(e) && ! Terran.SiegeTankSieged(e)))(_.pixelDistanceCenter(unit.pixel)).foreach(foe => {
      unit.agent.act("DuelWalk")
      unit.agent.forceTo(foe.pixel)
      Commander.move(unit)
    })
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
