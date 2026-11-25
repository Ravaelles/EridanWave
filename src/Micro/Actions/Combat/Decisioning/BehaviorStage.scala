package Micro.Actions.Combat.Decisioning

import Micro.Actions.UnitControl
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.DispatchBand

/**
 * Dispatched behavior contract (no game beyond the unit): one abstract
 * parent for every stage in every pipeline. Each stage states which
 * units it ever applies to; the shared template checks the gate first
 * and runs the doctrine second. Nobody overrides the template, nobody
 * dispatches a bare lambda - a new stage without a gate does not
 * compile, and the registry suite fails on a stage without coverage.
 * Reorder stages by moving the object, disable by commenting one out.
 *
 * The verdict type is the second half of the contract (owner order):
 * execute answers in UnitControl, never a bare boolean, so a stage
 * that forgets its verdict does not compile. Claiming credits the
 * stage name to the decision chain beside it - visibility is part of
 * claiming, not an afterthought.
 *
 * The third half is the reason (owner order, phase 1): WHY this stage
 * wants the frame, from the closed UnitIntentionReason vocabulary.
 * The shared template stamps it on the unit before executing, so every
 * BWAPI bid the stage posts below carries its reason into the resolve -
 * the Commander owns the rank table, behaviors never pass numbers.
 */
abstract class BehaviorStage(val name: String, val band: DispatchBand, val reason: Micro.Agency.UnitIntentionReason.Reason) {

  /**
   * Unit context gate (no game beyond the unit itself): race and class,
   * ground or air, worker or warrior, mobile or held, loaded or free.
   * Map, squad, targets and verdicts stay in execute - this answers
   * only which hulls may ever run the stage.
   */
  def appliesToUnit(unit: FriendlyUnitInfo): Boolean

  /**
   * Unit subset gate (no game beyond the class): which family this
   * stage answers for, enforced beside the unit-context gate below.
   * Movers stay universal - a stage that walks its hulls must never
   * strand loners - while rejoiners answer for the line alone.
   */
  def unitSubset: Micro.Actions.UnitSubset

  protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): UnitControl

  final def shouldRun(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): UnitControl = {
    if ( ! appliesToUnit(unit)) return UnitControl.Declined
    if ( ! unitSubset.contains(unit.unitClass)) return UnitControl.Declined

    unit.agent.bidReason = Some(reason)
    val verdict = execute(unit, combat, advance)
    if (verdict.claimed) unit.agent.act(name)
    verdict
  }
}
