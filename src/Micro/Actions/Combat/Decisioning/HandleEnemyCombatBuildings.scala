package Micro.Actions.Combat.Decisioning

import Micro.Actions.Combat.Decisioning.StaticStandoffBehavior
import Micro.Actions.Combat.Maneuvering.ArtilleryKeepOff
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.DispatchBand

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

  /**
   * Shells first: leave now. Any mobile grounded hull except Reavers -
   * the cloak, the verdict and the guns gate below.
   */
  object ShellsClaim extends TechniqueBehaviorStage("shells", DispatchBand.High) {
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean =
      ! unit.flying && unit.canMove && ! Protoss.Reaver(unit)

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      ArtilleryKeepOff.act(unit)
  }

  /**
   * Anti-air reach second: scouts hold outside it instead of diving in.
   * Scouts only; the umbrella math gates below.
   */
  object KeepOffClaim extends TechniqueBehaviorStage("keep-off", DispatchBand.High) {
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = Protoss.Scout(unit)

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      advance && ScoutStaticKeepOff.run(unit, combat)
  }

  /**
   * Contain-stand third: hold the 9.1 line and shoot out of it. Ranged
   * ground warriors, never Reavers; the cluster heat and the duel own
   * the rest.
   */
  object StandClaim extends TechniqueBehaviorStage("stand", DispatchBand.High) {
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean =
      unit.canMove && ! unit.flying && ! unit.unitClass.melee && unit.unitClass.isWarrior && ! Protoss.Reaver(unit)

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
      StaticStandoffBehavior.run(unit, combat)
  }

  private val claims: Vector[TechniqueBehaviorStage] = Vector(
    ShellsClaim,
    KeepOffClaim,
    StandClaim)

  def run(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
    claims.exists(_.shouldRun(unit, combat, advance))
}
