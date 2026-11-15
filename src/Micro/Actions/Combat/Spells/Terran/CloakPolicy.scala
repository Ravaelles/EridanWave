package Micro.Actions.Combat.Spells.Terran

import Lifecycle.With
import ProxyBwapi.Races.Terran
import ProxyBwapi.Techs.Tech
import ProxyBwapi.UnitClasses.UnitClass
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Cloak policy (one table for both hulls, no game access in the table
 * itself): which tech each hull decloaks with, how much energy it keeps
 * in reserve, and the full cloak/uncloak gates. The Wraith used to pass
 * the Ghost's tech to decloak - the parameter was ignored, so the bug
 * was invisible; the table makes the pairing checkable.
 */
object CloakPolicy {

  case class Hull(
    unitClass: UnitClass,
    tech: Tech,
    energyReserve: Int,
    cloak: FriendlyUnitInfo => Boolean,
    uncloak: FriendlyUnitInfo => Boolean)

  val hulls: Vector[Hull] = Vector(
    Hull(Terran.Ghost, Terran.GhostCloak, 30, ghostCloak, ghostUncloak),
    Hull(Terran.Wraith, Terran.WraithCloak, 20, wraithCloak, wraithUncloak))

  def techFor(unitClass: UnitClass): Option[Tech] =
    hulls.find(_.unitClass == unitClass).map(_.tech)

  def hullFor(unit: UnitInfo): Option[Hull] =
    hulls.find(h => unit.isAny(h.unitClass))

  private def ghostCloak(unit: FriendlyUnitInfo): Boolean = (
    unit.player.hasTech(Terran.GhostCloak)
    && Terran.Ghost(unit)
    && ! unit.cloaked
    && unit.energy >= Terran.GhostCloak.energyCost + 30
    && unit.matchups.framesOfSafety < 12 + With.reaction.agencyAverage
    && ! With.grids.enemyDetection.inRange(unit.tile))

  private def ghostUncloak(unit: FriendlyUnitInfo): Boolean = (
    Terran.Ghost(unit)
    && unit.cloaked
    && ! unit.matchups.enemies.exists(_.unitClass.attacksGround)
    && With.framesSince(unit.agent.lastCloak) > 24 * 4)

  private def wraithCloak(unit: FriendlyUnitInfo): Boolean = (
    unit.player.hasTech(Terran.WraithCloak)
    && Terran.Wraith(unit)
    && ! unit.cloaked
    && unit.confidence11 < 0.9
    && unit.matchups.pixelsToThreatRange.exists(_ < 32)
    && unit.energy >= Terran.WraithCloak.energyCost + 20
    && ! With.grids.enemyDetection.inRange(unit.tile))

  private def wraithUncloak(unit: FriendlyUnitInfo): Boolean = (
    Terran.Wraith(unit)
    && unit.cloaked
    && ! unit.matchups.enemies.exists(_.unitClass.attacksAir)
    && With.framesSince(unit.agent.lastCloak) > 24 * 10)
}
