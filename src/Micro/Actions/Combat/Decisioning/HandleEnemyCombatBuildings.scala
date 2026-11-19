package Micro.Actions.Combat.Decisioning

import Micro.Actions.Combat.Decisioning.StaticStandoffBehavior
import Micro.Actions.Combat.Maneuvering.ArtilleryKeepOff
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Enemy combat buildings, one home: everything a unit does about a
 * static defense lives in one first-claims list instead of scattered
 * across the dispatch chain. The list below IS the doctrine - shells
 * first (leave now), anti-air reach second (hold outside it),
 * contain-stand third (hold the 9.1 line and shoot out of it) - so a
 * wrong reorder is a visible diff to one place, never a silent drift
 * between two call sites. What the legs do not answer - duels,
 * formation, retreat - stays with the owners below; this gate only
 * narrows, never widens, and Retreat still owns fleeing outright.
 * Legs keep their kernels and suites; the group owns the order, and
 * the order is covered in StarEngine (ArtilleryKeepOffScenarioSuite,
 * ScoutStandoffScenarioSuite).
 */
object HandleEnemyCombatBuildings {

  private type StaticClaim = (FriendlyUnitInfo, Combat, Boolean) => Boolean

  private val claims: Seq[(String, StaticClaim)] = Seq(
    "shells"   -> ((unit: FriendlyUnitInfo, combat: Combat, advance: Boolean) => ArtilleryKeepOff.act(unit)),
    "keep-off" -> ((unit: FriendlyUnitInfo, combat: Combat, advance: Boolean) => advance && ScoutStaticKeepOff.run(unit, combat)),
    "stand"    -> ((unit: FriendlyUnitInfo, combat: Combat, advance: Boolean) => StaticStandoffBehavior.run(unit, combat)))

  def run(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
    claims.exists { case (_, claim) => claim(unit, combat, advance) }
}
