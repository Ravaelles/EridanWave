package Micro.Actions.Protoss.Reaver

import Lifecycle.With
import Micro.Actions.Protoss.ReaverLegacy.BeReaverLegacy
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
  tankAiming      : Boolean = false,
  holdTarget      : Boolean = false,
  targetPath      : Boolean = false,
  toAttackPath    : Boolean = false)

object ReaverThreats {

  def read(unit: FriendlyUnitInfo): ReaverThreats = {
    val latency       = With.latency.latencyFrames
    val cooldownReset = BeReaverLegacy.cooldownOnLanding // Engine resets to 30 on drop
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
      tankAiming      = threats.exists(t => Terran.SiegeTankSieged(t) && unit.tileDistanceEdge(t) < 10),
      holdTarget      = targetNearest.exists(t => ! t.flying && unit.canAttack(t) && unit.tileDistanceEdge(t) < 7.25),
      targetPath      = targetNearest.exists(unit.hasScarabPathTo(_, unit.pixel)),
      toAttackPath    = unit.agent.toAttack.orElse(targetNearest).exists(unit.hasScarabPathTo(_, unit.pixel)))
  }
}
