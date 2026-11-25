package Lifecycle

import Debugging._
import Information.Battles.KillLedger
import Utilities.?


/**
 * End-of-game reporting (was EridanWave.onEnd's first half): the score box
 * on stdout (Atlantis parity - the client log tees it to the terminal),
 * the details line, and teardown for every subsystem that keeps per-game
 * state outside the PerGame sweep (audit files, visualizer, tuning rows,
 * error counts). One call, no exit policy - that lives in SessionExit.
 */
object EndOfGame {

  def report(isWinner: Boolean): Unit = {
    With.logger.debug(f"Game ended in ${?(isWinner, "victory", "defeat")}")
    With.history.onEnd(isWinner)
    With.missionHistory.onEnd(isWinner)
    With.gathering.onEnd()
    With.storyteller.onEnd()
    With.simulation.onEnd()
    With.manners.onEnd(isWinner)
    // Atlantis GameSummary parity: the run ends with a score box on stdout
    // (the client log tees it to the terminal), not just the launcher line.
    System.out.println(GameSummary.box(
      isWinner,
      KillLedger.killed,
      KillLedger.lost))
    System.out.println(GameSummary.details(
      With.frame,
      KillLedger.killed,
      KillLedger.lost,
      KillLedger.killedResources,
      KillLedger.lostResources,
      HitCounter.total))
    // Rivalry ledgers (counted all game, reported once): starved duels
    // and phase-mismatch streaks, or the all-clear.
    System.out.println(DuelStarvation.summary())
    System.out.println(MotionMismatch.summary())
    System.out.println(VolleyWaste.summary())
    System.out.println(HitsTaken.summary())
    // Run terminator, last on stdout: who it was and when it ended.
    val now = java.time.LocalTime.now()
    System.out.println(GameSummary.matchOver(
      isWinner,
      ?(With.enemies.nonEmpty, With.enemies.map(_.name).mkString("+"), With.history.currentEnemyName),
      now.getHour,
      now.getMinute))
    FormationAudit.finish(With.mapFileName, With.frame)
    CherryVis.CherryVis.finish(isWinner)
    // Tuning score row (fixed-name + dated copy) when a strategy is active.
    Tuning.Tuning.finish(isWinner)
    // Keyed error counts next to the human log (no-op when nothing failed).
    ErrorLedger.finish()
    // Terminate the combat visualizer if we launched it at start
    try { if (With.configuration != null && With.configuration.debugging && With.configuration.simviz) Debugging.CombatVisIO.terminateVisualizer() } catch { case exception: Exception => With.logger.quietlyOnException(exception) }
    With.logger.flush()
  }
}
