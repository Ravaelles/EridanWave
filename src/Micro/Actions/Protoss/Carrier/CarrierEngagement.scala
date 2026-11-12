package Micro.Actions.Protoss.Carrier

import Micro.Targeting.CarrierBrawl

/**
 * Carrier engagement order: which of the five leash states (plus brawl, tank
 * hunt and launch) a Carrier takes this frame. Pure decision on precomputed
 * margins, so the state machine can be argued with in a test instead of in a
 * Carrier fight; BeCarrier gathers the readings and only executes.
 */
object CarrierEngagement {

  sealed trait CarrierOrder

  case object Brawl extends CarrierOrder

  case object TankHunt extends CarrierOrder

  case object Chase extends CarrierOrder

  case object Attack extends CarrierOrder

  case object OpenLeash extends CarrierOrder

  case object HoldLeash extends CarrierOrder

  case object Launch extends CarrierOrder

  case object WarmUp extends CarrierOrder

  final case class CarrierMargins(
    hasTarget           : Boolean,
    brawling            : Boolean,
    tankHunt            : Boolean,
    interceptorsActive  : Boolean,
    chasingAlready      : Boolean,
    safetyTiles         : Double,
    targetDistTiles     : Double)

  /** First matching rule wins, in the historical priority order. */
  def decide(m: CarrierMargins): CarrierOrder = {
    if ( ! m.hasTarget)                                                     WarmUp
    else if (m.brawling)                                                    Brawl
    else if (m.tankHunt)                                                    TankHunt
    else if ( ! m.interceptorsActive)                                       Launch
    else if ( ! m.chasingAlready
      && (m.safetyTiles > 1.0 || m.targetDistTiles > CarrierBrawl.launchTiles)) Chase
    else if (m.safetyTiles > 0)                                             Attack
    else if (m.targetDistTiles < CarrierBrawl.openLeashTiles)               OpenLeash
    else                                                                    HoldLeash
  }
}
