package Micro.Actions.Combat.Decisioning

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
 */
abstract class BehaviorStage(val name: String, val band: DispatchBand) {

  /**
   * Unit context gate (no game beyond the unit itself): race and class,
   * ground or air, worker or warrior, mobile or held, loaded or free.
   * Map, squad, targets and verdicts stay in execute - this answers
   * only which hulls may ever run the stage.
   */
  def appliesToUnit(unit: FriendlyUnitInfo): Boolean

  protected def execute(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean

  /**
   * Chain credit lives here, not in the dispatch loops: the old
   * per-Behavior act() died with the W1 migration and the Act,
   * BehaviorLog and CherryVis chains went dark for claimed frames.
   * Crediting the winning stage name restores the trace for every
   * pipeline at once - diagnostics only, never doctrine (no reader
   * but visuals, death logs and the frame-limit log looks at it).
   */
  final def shouldRun(unit: FriendlyUnitInfo, combat: Combat, advance: Boolean): Boolean = {
    if ( ! appliesToUnit(unit)) return false

    val claimed = execute(unit, combat, advance)
    if (claimed) unit.agent.act(name)
    claimed
  }
}
