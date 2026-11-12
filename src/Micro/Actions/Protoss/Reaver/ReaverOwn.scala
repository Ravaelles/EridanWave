package Micro.Actions.Protoss.Reaver

import Lifecycle.With
import Micro.Actions.Protoss.ReaverLegacy.BeReaverLegacy
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.?

/**
 * The Reaver's own state: hull, ride, cooldown and production. One of the
 * three one-frame reads behind ReaverSituation; threats and mission live in
 * ReaverThreats and ReaverMission. Field defaults describe a quiet Reaver.
 */
case class ReaverOwn(
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
  framesSinceFired: Int = 999999,
  walkableTile    : Boolean = true,
  traineeSoon     : Boolean = false)

object ReaverOwn {

  def read(unit: FriendlyUnitInfo): ReaverOwn = {
    val latency       = With.latency.latencyFrames
    val cooldownReset = BeReaverLegacy.cooldownOnLanding // Engine resets to 30 on drop

    ReaverOwn(
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
      framesSinceFired= With.framesSince(unit.lastFrameStartingAttack),
      walkableTile    = unit.tile.walkable,
      traineeSoon     = unit.trainee.exists(_.remainingCompletionFrames < cooldownReset + latency))
  }
}
