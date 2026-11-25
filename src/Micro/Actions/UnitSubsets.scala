package Micro.Actions

import ProxyBwapi.Races.{Protoss => RacesProtoss}
import ProxyBwapi.UnitClasses.UnitClass

/**
 * Unit subsets (no game access): the coarse vocabulary every brain and
 * behavior answers in addition to its own gate. A pack-assuming leg
 * reads the line subsets; raiders, transports and eyes never belong to
 * them, so a future ClusterRejoin cannot re-learn the DarkTemplar
 * lesson - the compiler knows the family, the suite pins the members,
 * and the Action template enforces the pick every frame.
 */
sealed trait UnitSubset {

  def contains(unitClass: UnitClass): Boolean
}

object UnitSubsets {

  /** Every unit: the default, preserves the caller's own gate untouched. */
  case object AllUnits extends UnitSubset {
    def contains(unitClass: UnitClass): Boolean = true
  }

  /** Grounded hulls and workers. */
  case object GroundUnits extends UnitSubset {
    def contains(unitClass: UnitClass): Boolean = ! unitClass.isFlyer
  }

  /** Flying hulls and eyes. */
  case object AirUnits extends UnitSubset {
    def contains(unitClass: UnitClass): Boolean = unitClass.isFlyer
  }

  /** Units with a weapon of any kind. */
  case object ArmedUnits extends UnitSubset {
    def contains(unitClass: UnitClass): Boolean = unitClass.attacksOrCasts
  }

  /** Workers, and only workers. */
  case object Workers extends UnitSubset {
    def contains(unitClass: UnitClass): Boolean = unitClass.isWorker
  }

  /**
   * The pack line (RegularProtossArmy): warriors that fight in formation.
   * Lone raiders never belong to it - DarkTemplar hunt alone, Corsairs
   * and Scouts skirmish alone - and neither do transports and eyes, which
   * are not warriors to begin with. Carriers, Reavers, Arbiters and High
   * Templar stay in: they travel with the ball even when they shoot over
   * it, and no game has shown them wedged by a rejoin yet.
   */
  case object LineArmy extends UnitSubset {
    def contains(unitClass: UnitClass): Boolean =
      unitClass.isWarrior &&
        unitClass != RacesProtoss.DarkTemplar &&
        unitClass != RacesProtoss.Corsair &&
        unitClass != RacesProtoss.Scout
  }
}
