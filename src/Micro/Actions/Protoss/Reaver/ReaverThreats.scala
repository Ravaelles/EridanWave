package Micro.Actions.Protoss.Reaver

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * What threatens the Reaver this frame: incoming fire, wounds and targets.
 * One of the three one-frame reads behind ReaverSituation; own state and
 * mission live in ReaverOwn and ReaverMission.
 */
case class ReaverThreats(
  targetedByScarab: Boolean = false,
  framesSinceHit  : Int = 999999,
  woundFraction   : Double = 0.0,
  threatened      : Boolean = false,
  cooling         : Boolean = false,
  outranged       : Boolean = false,
  shotless        : Boolean = false,
  pressuredClose  : Boolean = false,
  staticInFace    : Boolean = false,
  tankAiming      : Boolean = false,
  holdTarget      : Boolean = false,
  onlyWorkersNear : Boolean = false,
  targetPath      : Boolean = false,
  toAttackPath    : Boolean = false,
  foeInReach      : Boolean = false,
  tankCovered     : Boolean = false)

object ReaverThreats {

  /** Enemies this close, in tiles, are engaged instead of fled. */
  val engageTiles: Double = 8.5

  /** Sieged-tank range plus this margin, in tiles, is never entered. */
  val tankMarginTiles: Double = 1.0

  /** A foe this close is worth standing and shooting at. */
  def inReach(distTiles: Double): Boolean =
    distTiles <= engageTiles

  /** Inside a sieged tank's reach plus the margin means the stand is suicide. */
  def tankCovered(tankRangeTiles: Double, distTiles: Double): Boolean =
    distTiles <= tankRangeTiles + tankMarginTiles

  def read(unit: FriendlyUnitInfo): ReaverThreats = {
    val latency       = With.latency.latencyFrames
    val cooldownReset = ReaverActions.cooldownOnLanding
    val threatsInRange= unit.matchups.threatsInRange
    val threats       = unit.matchups.threats
    val targetNearest = unit.matchups.targetNearest

    ReaverThreats(
      targetedByScarab= unit.matchups.targetedByScarab,
      framesSinceHit  = With.framesSince(unit.lastFrameTakingDamage),
      woundFraction   = unit.woundPercent(),
      threatened      = threatsInRange.nonEmpty,
      cooling         = unit.cooldownLeft > cooldownReset + 2 * latency,
      outranged       = threatsInRange.exists(t => ! t.flying && t.pixelRangeAgainst(unit) < unit.pixelRangeAgainst(t)),
      shotless        = ! (unit.scarabs > 0 && ! unit.hasActiveScarab && unit.matchups.targetsInRange.exists(unit.hasScarabPathTo(_, unit.pixel))),
      pressuredClose  = threats.exists(t => ! t.flying && t.canAttack(unit) && unit.tileDistanceEdge(t) < 4),
      staticInFace    = threats.exists(t =>
        t.unitClass.isStaticDefense
        && ! t.flying
        && t.likelyStillThere
        && t.canAttack(unit)
        && ReaverStaticStandoff.shouldBackOff(
          unit.tileDistanceEdge(t),
          unit.pixelRangeAgainst(t) * Maff.inv32,
          t.pixelRangeAgainst(unit) * Maff.inv32)),
      onlyWorkersNear = {
        val nearby = unit.matchups.enemies.filter(e => unit.tileDistanceEdge(e) < ReaverBoardLogic.workerStandRadiusTiles)
        nearby.nonEmpty && nearby.forall(_.unitClass.isWorker)
      },
      tankAiming      = threats.exists(t => Terran.SiegeTankSieged(t) && unit.tileDistanceEdge(t) < 10),
      holdTarget      = targetNearest.exists(t => ! t.flying && unit.canAttack(t) && unit.tileDistanceEdge(t) < 7.25),
      targetPath      = targetNearest.exists(unit.hasScarabPathTo(_, unit.pixel)),
      toAttackPath    = unit.agent.toAttack.orElse(targetNearest).exists(unit.hasScarabPathTo(_, unit.pixel)),
      foeInReach      = unit.matchups.targets.exists(t =>
        ! t.flying && unit.canAttack(t) && inReach(unit.tileDistanceEdge(t))),
      tankCovered     = threats.exists(t =>
        Terran.SiegeTankSieged(t) &&
          tankCovered(t.pixelRangeAgainst(unit) * Maff.inv32, unit.tileDistanceEdge(t))))
  }
}
