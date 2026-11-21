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
 */
abstract class BehaviorStage(val name: String, val band: DispatchBand) {

  /**
   * Unit context gate (no game beyond the unit itself): race and class,
   * ground or air, worker or warrior, mobile or held, loaded or free.
   * Map, squad, targets and verdicts stay in execute - this answers
   * only which hulls may ever run the stage.
   */
  def appliesToUnit(unit: FriendlyUnitInfo): Boolean

  protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): UnitControl

  final def shouldRun(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): UnitControl = {
    if ( ! appliesToUnit(unit)) return UnitControl.Declined

    val verdict = execute(unit, combat, advance)
    if (verdict.claimed) unit.agent.act(name)
    verdict
  }
}
