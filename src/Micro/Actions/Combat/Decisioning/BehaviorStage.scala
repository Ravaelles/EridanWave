package Micro.Actions.Combat.Decisioning

import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.DispatchBand

/**
 * Dispatched behavior contract (no game beyond the unit): every entry
 * in every combat pipeline is one of these, never a bare lambda - so
 * there is exactly one place where a behavior states which units it
 * ever applies to, and the compiler rejects a behavior without it.
 * The unit gate runs first on every frame; situation logic (targets,
 * votes, bands) stays in execute beside the kernels that pin it.
 * Reorder stages by moving the object, disable by commenting one out -
 * order tables pin the sequence beside the rows.
 */
abstract class TechniqueBehaviorStage(val name: String, val band: DispatchBand) {

  /**
   * Unit context gate (no game beyond the unit itself): race and class,
   * ground or air, worker or warrior, mobile or not. Map, squad and
   * enemy state stay in execute - this answers only "could this hull
   * ever do this", for every hull, before any situation is read.
   */
  def appliesToUnit(unit: FriendlyUnitInfo): Boolean

  protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean

  final def shouldRun(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean = {
    if ( ! appliesToUnit(unit)) return false

    execute(unit, combat, advance)
  }
}

/**
 * Engage-pipeline behavior: same contract, the claim carries the
 * engage result instead of ending the frame.
 */
abstract class EngageBehaviorStage(val name: String, val band: DispatchBand) {

  def appliesToUnit(unit: FriendlyUnitInfo): Boolean

  protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Option[Boolean]

  final def shouldRun(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Option[Boolean] = {
    if ( ! appliesToUnit(unit)) return None

    execute(unit, combat, advance)
  }
}

/**
 * Duel-stage behavior: same contract, no advance gate - the duel
 * dispatch runs these once the engage is already claimed.
 */
abstract class DuelBehaviorStage(val name: String, val band: DispatchBand) {

  def appliesToUnit(unit: FriendlyUnitInfo): Boolean

  protected def execute(unit: FriendlyUnitInfo, combat: Combat): Boolean

  final def shouldRun(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! appliesToUnit(unit)) return false

    execute(unit, combat)
  }
}
