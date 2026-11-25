package Micro.Actions.Combat.Decisioning

import Micro.Actions.Combat.Decisioning.StaticStandoffBehavior
import Micro.Actions.Combat.Maneuvering.ArtilleryKeepOff
import Micro.Actions.UnitControl
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
  object ShellsClaim extends BehaviorStage("shells", DispatchBand.High, Micro.Agency.UnitIntentionReason.Dodge) {
    override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean =
      ! unit.flying && unit.canMove && ! Protoss.Reaver(unit)

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): UnitControl =
      UnitControl.claimedIf(ArtilleryKeepOff.act(unit))
  }

  /**
   * Anti-air reach second: scouts hold outside it instead of diving in.
   * Scouts only; the umbrella math gates below.
   */
  object KeepOffClaim extends BehaviorStage("keep-off", DispatchBand.High, Micro.Agency.UnitIntentionReason.Dodge) {
    override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean = Protoss.Scout(unit)

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): UnitControl =
      UnitControl.claimedIf(advance && ScoutStaticKeepOff.run(unit, combat))
  }

  /**
   * Contain-stand third: hold the matchup line and shoot out of it (7.8
   * vs Protoss, 9.1 vs Zerg, 6.6 vs Terran, 15.2 under siege). Ranged
   * ground warriors, never Reavers; the cluster heat and the duel own
   * the rest.
   */
  object StandClaim extends BehaviorStage("stand", DispatchBand.High, Micro.Agency.UnitIntentionReason.Duel) {
    override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
    def appliesToUnit(unit: FriendlyUnitInfo): Boolean =
      unit.canMove && ! unit.flying && ! unit.unitClass.melee && unit.unitClass.isWarrior && ! Protoss.Reaver(unit)

    protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): UnitControl =
      UnitControl.claimedIf(StaticStandoffBehavior.run(unit, combat))
  }

  private val claims: Vector[BehaviorStage] = Vector(
    ShellsClaim,
    KeepOffClaim,
    StandClaim)

  def order: Seq[String] =
    claims.map(_.name)

  def run(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean =
    claims.exists(_.shouldRun(unit, combat, advance).claimed)
}
