package Micro.Actions

import Debugging.ToString
import Lifecycle.With
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

abstract class Action {

  val name: String = ToString(this)

  protected def requiresReadiness: Boolean = true

  def allowed(unit: FriendlyUnitInfo): Boolean = true

  /**
   * Unit subset gate (no game beyond the class): the coarse family this
   * brain answers for, enforced beside the unit-context gate below. The
   * default keeps the caller's own gate untouched; pack-assuming legs
   * narrow it to the line so a future rejoin cannot re-learn the lone
   * raider lesson - Dark Templars, Corsairs and Scouts never belong to
   * a centroid they do not fight in.
   */
  def unitSubset: UnitSubset

  /**
   * Unit context gate (no game beyond the unit itself): race and class,
   * ground or air, worker or warrior, mobile or held. The shared template
   * checks the gate first and runs the doctrine second - a brain without
   * a gate does not belong in the chain. Game and map reads stay in
   * allowed and perform.
   */
  def appliesToUnit(unit: FriendlyUnitInfo): Boolean = true

  /**
   * Verdict contract (owner order): every unit-controlling action states
   * whether it owned the frame. The compiler lists every override, so a
   * new action that forgets its verdict does not compile. Claimed frames
   * carry the action's name into the decision chain beside the behavior
   * stages - no silent orders. Delegating containers pass through
   * (their leaves report); order-issuing leaves claim their frames.
   */
  protected def perform(unit: FriendlyUnitInfo): UnitControl

  /**
   * Chain containers report through their leaves, never under their own
   * name: override this in containers (Fight, Transport, Cast, ...) so
   * adding one never means editing a trace filter in another package.
   */
  def isContainer: Boolean = false

  final def apply(unit: FriendlyUnitInfo): Boolean = {
    act(unit, giveCredit = ! isContainer)
  }

  final def delegate(unit: FriendlyUnitInfo): Boolean = {
    act(unit, giveCredit = false)
  }

  final def act(unit: FriendlyUnitInfo, giveCredit: Boolean): Boolean = {
    With.agents.timeAction(name) {
      if (( ! requiresReadiness || unit.ready) && appliesToUnit(unit) && unitSubset.contains(unit.unitClass) && allowed(unit)) {
        val previousCredit = unit.agent.lastAction
        val verdict = perform(unit)
        if (giveCredit && verdict.claimed) unit.agent.act(name)
        if (With.configuration.debugging && ! isContainer && verdict.claimed) {
          unit.agent.actions += name
        }
        if (unit.ready) {
          previousCredit.foreach(unit.agent.act)
        }
      }
    }
    unit.unready
  }
}
