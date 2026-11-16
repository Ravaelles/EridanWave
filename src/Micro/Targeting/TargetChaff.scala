package Micro.Targeting

import ProxyBwapi.Races.Zerg
import ProxyBwapi.UnitClasses.UnitClass

/**
 * Hatch chaff (no game access): Larvae and Eggs are never combat targets -
 * they cannot fight, and a firing line chewing them trades shots for
 * nothing while real threats close. One predicate so the registry, the
 * target filter and the stale-target release never disagree on what
 * counts. Morphing combat units (Cocoons, Lurker Eggs) are valid targets
 * and stay out of this.
 */
object TargetChaff {

  def isChaff(unitClass: UnitClass): Boolean =
    unitClass == Zerg.Larva || unitClass == Zerg.Egg
}
