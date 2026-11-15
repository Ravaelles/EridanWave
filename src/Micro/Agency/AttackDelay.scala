package Micro.Agency

import Lifecycle.With
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

// Order timing after an attack: withhold the next order until the unit can
// safely receive it, or the attack sticks. The archaeology lives in
// .ai/attack-delay.md; this file keeps the rule and the numbers.
object AttackDelay {

  def framesToWaitAfterIssuingAttackOrder(unit: FriendlyUnitInfo): Int = {
    if (Protoss.Carrier(unit)) {
      // Need a better answer than this (see queue.md: carrier answer)

      return 24
    }
    // Sleep through command delivery so the next frame can observe and protect an attack that started.
    With.latency.remainingFrames + 1
  }

  // OpenBW applies commands before running iscripts. After nobrkcodestart has run, follow-up orders queue until
  // nobrkcodeend instead of interrupting the attack. These offsets are the first safe command-arrival frames,
  // relative to the frame on which the attack cooldown starts. Lurkers and Hydralisks use the end of their
  // interruptible repeat animation because cooldown alone does not distinguish an initial attack from a repeat.
  private lazy val safeOrderArrivalFramesByUnit = Map(
    Protoss.Corsair     -> 1,
    Zerg.SunkenColony   -> 1,
    Protoss.Arbiter     -> 2,
    Protoss.Archon      -> 2,
    Protoss.DarkTemplar -> 2,
    Terran.Valkyrie     -> 2,
    Zerg.Broodling      -> 2,
    Zerg.Ultralisk      -> 2,
    Zerg.Zergling       -> 2,
    Protoss.Zealot      -> 3,
    Zerg.Lurker         -> 3,
    Terran.Ghost        -> 4,
    Terran.Marine       -> 4,
    Zerg.Hydralisk      -> 4)

  def safeToIssueOrder(unit: FriendlyUnitInfo): Boolean = {
    // The +2 is to make units stick less after an attack
    // I tested this prior to COG 2021.
    // +0 lead to a lot of sticking.
    // +1 lead to some sticking, enough to lose games because of it.
    // +2 appeared to prevent all sticking
    val safeArrivalFrame = unit.lastFrameStartingAttack + safeOrderArrivalFramesByUnit.getOrElse(unit.unitClass, unit.unitClass.stopFrames + 2)
    With.frame + With.latency.remainingFrames >= safeArrivalFrame
  }
}
