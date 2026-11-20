package Strategery

import Strategery.Strategies.Protoss.{PvPReaver, PvPRobo}
import Strategery.Strategies.Strategy

/**
 * Temporary testing switches (one home for "is this gameplan family
 * even allowed"): consulted by `StrategyLegality`, so a disabled family
 * never wins selection, and by `ProductionPolicy`, so its units never
 * queue either. The static list (`Playbook.disabled`) stays the
 * permanent mechanism; these vars are the temporary one - flip, run
 * games, revert. Defaults come from the owner, not from caution:
 * everything stays enabled unless a test series says otherwise.
 */
object StrategyToggles {

  /**
   * TEMPORARY (PvP Reaver testing, 2026-11): PvP Robo gameplans off, so
   * selection falls through to goon/DT/expand play and the late game
   * answers with Templar tech instead. `PvPObs` stays legal on purpose:
   * killing mobile detection too would blind us to Dark Templars with
   * nothing but cannons left - one line below if a series wants it all.
   */
  var roboGameplansEnabled: Boolean = false

  /**
   * TEMPORARY (PvP Reaver testing, 2026-11): Reaver hulls and their Bay
   * never queue in a mirror, even if a Robo somehow stands. Scoped to
   * PvP inside the policy; PvT/PvZ/PvR Reavers flow untouched.
   */
  var reaverProductionEnabled: Boolean = false

  /** Robo gameplan family: the travelling pair plus its tech choice. */
  private def isRoboGameplan(strategy: Strategy): Boolean =
    strategy == PvPRobo || strategy == PvPReaver

  /** Selection gate: everything flows except a disabled family. */
  def allows(strategy: Strategy): Boolean =
    if ( ! roboGameplansEnabled && isRoboGameplan(strategy)) false
    else true
}
