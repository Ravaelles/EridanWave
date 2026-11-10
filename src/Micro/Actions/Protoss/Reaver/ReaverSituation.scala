package Micro.Actions.Protoss.Reaver

import Lifecycle.With
import Micro.Actions.Combat.Decisioning.Skirmish.FightOrFlee
import ProxyBwapi.UnitTracking.AliveEnemies
import Micro.Actions.Protoss.ReaverLegacy.BeReaverLegacy
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.?

/**
 * One-frame Reaver snapshot: every game read the Atlantis discipline needs,
 * collected once. Behaviors below combine these facts but never touch the
 * game themselves, so they stay unit-testable with hand-built literals.
 * Field defaults describe a quiet Reaver; tests override what they probe.
 */
case class ReaverSituation(
  rideNearby      : Boolean = false,
  loaded          : Boolean = false,
  hasTransport    : Boolean = false,
  committed       : Boolean = false,
  shieldsDown     : Int = 0,
  scarabs         : Int = 0,
  hasActiveScarab : Boolean = false,
  cooldownLeft    : Int = 0,
  latencyFrames   : Int = 0,
  doomed          : Boolean = false,
  targetedByScarab: Boolean = false,
  framesSinceFired: Int = 999999,
  framesSinceHit  : Int = 999999,
  hikingFar       : Boolean = false,
  woundFraction   : Double = 0.0,
  isTerran        : Boolean = false,
  reaveOrdered    : Boolean = false,
  shouldFight     : Boolean = false,
  walkableTile    : Boolean = true,
  traineeSoon     : Boolean = false,
  leadLanding     : Boolean = false,
  threatened      : Boolean = false,
  cooling         : Boolean = false,
  outranged       : Boolean = false,
  shotless        : Boolean = false,
  pressuredClose  : Boolean = false,
  tankAiming      : Boolean = false,
  holdTarget      : Boolean = false,
  targetPath      : Boolean = false,
  toAttackPath    : Boolean = false,
  braveDrop       : Boolean = false,
  unloadSpotSafe  : Boolean = true) {

  /** A threat in range or close enough to hit us (derived, never stale). */
  def pressured: Boolean = threatened || pressuredClose
}

object ReaverSituation {

  def read(unit: FriendlyUnitInfo): ReaverSituation = {
    val latency       = With.latency.latencyFrames
    val cooldownReset = BeReaverLegacy.cooldownOnLanding // Engine resets to 30 on drop
    val threatsInRange= unit.matchups.threatsInRange
    val threats       = unit.matchups.threats
    val targetNearest = unit.matchups.targetNearest
    val wound         = unit.woundPercent()
    val threatenedNow = threatsInRange.nonEmpty
    val pressuredCloseNow = threats.exists(t => ! t.flying && t.canAttack(unit) && unit.pixelDistanceEdge(t) < 4 * 32)

    val isTerranNow = With.enemies.exists(_.isTerran)
    val firedAgo = With.framesSince(unit.lastFrameStartingAttack)
    val hasFiredShot = unit.lastFrameStartingAttack >= 0

    ReaverSituation(
      rideNearby      = unit.agent.ride.exists(_.pixelDistanceEdge(unit) <= ?(unit.agent.isPrimaryPassenger, 64, 32)),
      loaded          = unit.loaded,
      hasTransport    = unit.transport.isDefined,
      committed       = unit.agent.commit,
      shieldsDown     = unit.unitClass.maxShields - unit.shieldPoints,
      scarabs         = unit.scarabs,
      hasActiveScarab = unit.hasActiveScarab,
      cooldownLeft    = unit.cooldownLeft,
      latencyFrames   = latency,
      doomed          = unit.cooldownLeft >= unit.doomedInFrames,
      targetedByScarab= unit.matchups.targetedByScarab,
      framesSinceFired= firedAgo,
      framesSinceHit  = With.framesSince(unit.lastFrameTakingDamage),
      hikingFar       = unit.moving && unit.pixelDistanceTravelling(unit.agent.destinationNext()) > 15 * 32,
      woundFraction   = wound,
      isTerran        = isTerranNow,
      reaveOrdered    = unit.agent.fightReason == FightOrFlee.Reave,
      shouldFight     = unit.agent.shouldFight,
      walkableTile    = unit.tile.walkable,
      traineeSoon     = unit.trainee.exists(_.remainingCompletionFrames < cooldownReset + latency),
      leadLanding     = unit.agent.ride.exists(_.agent.passengersPrioritized.headOption.exists(r =>
        Protoss.Reaver(r) && r != unit && r.agent.shouldFight && ! r.loaded && r.pixelDistanceEdge(unit) < 32)),
      threatened      = threatenedNow,
      cooling         = unit.cooldownLeft > cooldownReset + 2 * latency,
      outranged       = threatsInRange.exists(t => ! t.flying && t.pixelRangeAgainst(unit) < unit.pixelRangeAgainst(t)),
      shotless        = ! (unit.scarabs > 0 && ! unit.hasActiveScarab && unit.matchups.targetsInRange.exists(unit.hasScarabPathTo(_, unit.pixel))),
      pressuredClose  = pressuredCloseNow,
      tankAiming      = threats.exists(t => Terran.SiegeTankSieged(t) && unit.pixelDistanceEdge(t) < 10 * 32),
      holdTarget      = targetNearest.exists(t => ! t.flying && unit.canAttack(t) && unit.pixelDistanceEdge(t) < 7.25 * 32),
      targetPath      = targetNearest.exists(unit.hasScarabPathTo(_, unit.pixel)),
      toAttackPath    = unit.agent.toAttack.orElse(targetNearest).exists(unit.hasScarabPathTo(_, unit.pixel)),
      braveDrop       = unit.transport.exists(shuttle => {
        // Engine edge distances are pixels; thresholds are tiles (rule 10).
        // shuttle.matchups.targets is empty for an unarmed hull, so guns
        // aimed at the Shuttle come off the single registry instead.
        // Scourge rides along: alive and typed, weapon data or not.
        val distTiles = AliveEnemies.get().map(foe => (foe, foe.pixelDistanceEdge(shuttle) / 32.0))
        val foesClose = distTiles.count(_._2 < ReaverDropBravery.closeDist)
        val ranged = distTiles.filter(_._2 < ReaverDropBravery.rangedDist).filter({ case (foe, _) =>
          ReaverDropBravery.isRangedThreat(
            foe.unitClass.canAttack, foe.unitClass.melee, foe.flying, foe.canAttack(shuttle), Zerg.Scourge(foe)) })
        val rangedDps = ranged.map({ case (foe, _) => foe.dpfAir }).sum
        val tanks = AliveEnemies.get().count(t =>
          t.isAny(Terran.SiegeTankUnsieged, Terran.SiegeTankSieged) && t.likelyStillThere &&
            t.pixelDistanceEdge(shuttle) / 32.0 < ReaverDropBravery.tankTheaterTiles)
        val nearStatics = AliveEnemies.get().exists(e =>
          e.unitClass.isStaticDefense && ! e.flying && e.likelyStillThere &&
            e.pixelDistanceEdge(shuttle) / 32.0 < ReaverDropBravery.staticDefenseDist)
        val shieldsUp = shuttle.shieldPoints >= ReaverDropBravery.minShields && unit.shieldPoints >= ReaverDropBravery.minShields
        val affordable = ReaverDropBravery.canAffordShot(shuttle.shieldPoints + shuttle.hitPoints, rangedDps)
        val tankGate = ! isTerranNow || tanks <= ReaverDropBravery.maxTerranTanks
        val ringsOk = ReaverDropBravery.shouldDrop(
          foesClose, ranged.size, shuttle.shieldPoints, unit.shieldPoints,
          isTerran = isTerranNow, enemyTanks = tanks, nearStaticDefense = nearStatics) && affordable
        val staleOk = ReaverDropBravery.mayDropStale(firedAgo, wound, hasFired = hasFiredShot) &&
          shieldsUp && affordable && ! nearStatics && tankGate
        ringsOk || staleOk
      }),
      unloadSpotSafe  = ! threats.exists(t => ! t.flying && unit.pixelDistanceEdge(t) < ReaverUnloadSafety.radiusTiles(wound) * 32))
  }
}
