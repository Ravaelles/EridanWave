package Micro.Actions.Protoss.Reaver

import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * One-frame Reaver snapshot: every game read the Atlantis discipline needs,
 * collected once. The snapshot is read as three coherent records - own state
 * (ReaverOwn), threats (ReaverThreats) and mission (ReaverMission) - and
 * composed here, so the facade keeps its field names for the behaviors below
 * while each record stays small enough to reason about. Behaviors combine
 * these facts but never touch the game themselves, so they stay unit-testable
 * with hand-built literals.
 * Field defaults describe a quiet Reaver; tests override what they probe.
 */
case class ReaverSituation(
  rideNearby      : Boolean = false,
  loaded          : Boolean = false,
  hasTransport    : Boolean = false,
  committed       : Boolean = false,
  shieldsDown     : Int = 0,
  shields         : Int = 80,
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
  staticInFace    : Boolean = false,
  tankAiming      : Boolean = false,
  holdTarget      : Boolean = false,
  onlyWorkersNear : Boolean = false,
  targetPath      : Boolean = false,
  toAttackPath    : Boolean = false,
  foeInReach      : Boolean = false,
  tankCovered     : Boolean = false,
  braveDrop       : Boolean = false,
  containing      : Boolean = false,
  combatNear      : Boolean = false,
  unloadVetoed    : Boolean = false,
  unloadSpotSafe  : Boolean = true) {

  /** A threat in range or close enough to hit us (derived, never stale). */
  def pressured: Boolean = threatened || pressuredClose
}

object ReaverSituation {

  def read(unit: FriendlyUnitInfo): ReaverSituation = {
    val own     = ReaverOwn.read(unit)
    val threats = ReaverThreats.read(unit)
    val mission = ReaverMission.read(unit)

    ReaverSituation(
      rideNearby      = own.rideNearby,
      loaded          = own.loaded,
      hasTransport    = own.hasTransport,
      committed       = own.committed,
      shieldsDown     = own.shieldsDown,
      shields         = own.shields,
      scarabs         = own.scarabs,
      hasActiveScarab = own.hasActiveScarab,
      cooldownLeft    = own.cooldownLeft,
      latencyFrames   = own.latencyFrames,
      doomed          = own.doomed,
      targetedByScarab= threats.targetedByScarab,
      framesSinceFired= own.framesSinceFired,
      framesSinceHit  = threats.framesSinceHit,
      hikingFar       = mission.hikingFar,
      woundFraction   = threats.woundFraction,
      isTerran        = mission.isTerran,
      reaveOrdered    = mission.reaveOrdered,
      shouldFight     = mission.shouldFight,
      walkableTile    = own.walkableTile,
      traineeSoon     = own.traineeSoon,
      leadLanding     = mission.leadLanding,
      threatened      = threats.threatened,
      cooling         = threats.cooling,
      outranged       = threats.outranged,
      shotless        = threats.shotless,
      pressuredClose  = threats.pressuredClose,
      staticInFace    = threats.staticInFace,
      onlyWorkersNear = threats.onlyWorkersNear,
      tankAiming      = threats.tankAiming,
      holdTarget      = threats.holdTarget,
      targetPath      = threats.targetPath,
      toAttackPath    = threats.toAttackPath,
      foeInReach      = threats.foeInReach,
      tankCovered     = threats.tankCovered,
      braveDrop       = mission.braveDrop,
      containing      = mission.containing,
      combatNear      = mission.combatNear,
      unloadVetoed    = mission.unloadVetoed,
      unloadSpotSafe  = mission.unloadSpotSafe)
  }
}
