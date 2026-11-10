package Gameplans.Protoss.PvP.opening

import Debugging.SimpleString

/**
 * Shared mutable latch state of PvPOpening, one concept per flag group.
 * Split out so each behavior trait below reads and writes one visible
 * state holder instead of a 968-line class body. No logic here.
 */
trait PvPOpeningState {

  // Attack timings
  private[PvP] var complete            : Boolean = false
  private[PvP] var initialZealotAttack : Boolean = false
  private[PvP] var atEarlyTiming       : Boolean = false
  private[PvP] var atMainTiming        : Boolean = false
  private[PvP] var earlyTimingClosed   : Boolean = false
  private[PvP] var mainTimingClosed    : Boolean = false
  private[PvP] var timingVs2Gate       : Boolean = false
  private[PvP] var timingVsProxy       : Boolean = false
  private[PvP] var shouldExpand        : Boolean = false
  private[PvP] var shouldAttack        : Boolean = false
  private[PvP] var shouldHarass        : Boolean = false
  private[PvP] var shouldHoldNatural   : Boolean = false
  private[PvP] var had2GateBeforeCore  : Boolean = false
  private[PvP] var hadTechBeforeRange  : Boolean = false
  private[PvP] var rangeDelayed        : Boolean = false

  // 10-12
  private[PvP] var commitZealots       : Boolean = false
  private[PvP] var sevenZealot         : Boolean = false
  // 1 Gate Core
  private[PvP] var zBeforeCore         : Boolean = false
  private[PvP] var zAfterCore          : Boolean = false
  // Robo
  private[PvP] var getObservers        : Boolean = false
  private[PvP] var getObservatory      : Boolean = false
  private[PvP] var getReavers          : Boolean = false
  private[PvP] var reaverAllIn         : Boolean = false
  private[PvP] var shuttleFirst        : Boolean = false
  private[PvP] var shuttleSpeed        : Boolean = false
  private[PvP] var doSneakyRobo        : Boolean = true
  // DT
  private[PvP] var greedyDT            : Boolean = false
  private[PvP] var cannonExpand        : Boolean = false
  private[PvP] var speedlotAllIn       : Boolean = false

  trait CoreSequence  extends SimpleString
  object GCRange      extends CoreSequence
  object GCTech       extends CoreSequence
  object GCGate       extends CoreSequence
  object GGC          extends CoreSequence
  private[PvP] def gcrange : Boolean       = sequence == GCRange
  private[PvP] def gctech  : Boolean       = sequence == GCTech
  private[PvP] def gcgate  : Boolean       = sequence == GCGate
  private[PvP] def ggcore  : Boolean       = sequence == GGC
  private[PvP] var sequence: CoreSequence  = _
}
