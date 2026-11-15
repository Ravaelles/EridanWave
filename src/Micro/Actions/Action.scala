package Micro.Actions

import Debugging.ToString
import Lifecycle.With
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

abstract class Action {
  
  val name: String = ToString(this)
  private var performanceAgency: AnyRef = _
  private var performance: ActionPerformance = _

  protected def requiresReadiness: Boolean = true
  
  def allowed(unit: FriendlyUnitInfo): Boolean = true
  protected def perform(unit: FriendlyUnitInfo): Unit

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
    val nanosBefore = System.nanoTime()
    if (( ! requiresReadiness || unit.ready) && allowed(unit)) {
      val previousCredit = unit.agent.lastAction
      if (giveCredit) unit.agent.act(name)
      if (With.configuration.debugging && ! isContainer) {
        unit.agent.actions += name
      }
      perform(unit)
      if (unit.ready) {
        previousCredit.foreach(unit.agent.act)
      }
    }
    val performance = actionPerformance
    val nanosAfter = System.nanoTime()
    performance.durationNanos += Math.max(0, nanosAfter - nanosBefore)
    performance.invocations += 1
    unit.unready
  }

  @inline private def actionPerformance: ActionPerformance = {
    val agency = With.agents
    if (performanceAgency ne agency) {
      performanceAgency = agency
      performance = agency.actionPerformance.getOrElseUpdate(name, new ActionPerformance)
    }
    performance
  }
}
