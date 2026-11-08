package Lifecycle

import Debugging._
import Placement.JBWEBWrapper
import Utilities.?
import bwapi.{DefaultBWListener, Player}
import jbweb.Walls

class EridanWave extends DefaultBWListener {

  private def timeFrameZero(todo: () => Unit): Unit = {
    val msBefore = System.nanoTime / 1000000
    todo()
    if (With.frame == 0) {
      With.frame0ms += System.nanoTime / 1000000 - msBefore
    }
  }

  override def onStart(): Unit = {
    With.frame0ms = 0
    timeFrameZero(() => {
      tryCatch(() => {
        With.onStart()
        // Tuning params (if --tuning-strategy= was passed): randomize or
        // override before the first frame, Atlantis OnGameStarted parity.
        Tuning.Tuning.install()
        With.logger.debug(f"[keyboard] ${Lifecycle.Keyboard.Keyboard.status()}")
        Debugging.ErrorLedger.reset()
        Debugging.CherryVis.CherryVis.initialize()
        Walls.logInfo = With.configuration.logstd
        JBWEBWrapper.onStart()
        With.history.onStart()
        With.units.onStart()
        UmsMode.detectAndApply()
        With.grids.creep.update()
        With.geography.onStart()
        unpauseIfPaused()
      })})
  }

  override def onFrame(): Unit = {
    timeFrameZero(() => {
      tryCatch(() => {
        With.performance.startFrame()
        With.onFrame()
        unpauseIfPaused()
        With.latency.onFrame()
        With.units.onFrame()
        // Parked while paused: the engine is frozen by togglePause, but
        // onFrame keeps arriving, so tracking, chat commands, camera and
        // visualization keep running and the overlay never vanishes.
        // Decisions, orders and speed tweaks stay parked until resume.
        if ( ! Keyboard.GameCommands.isBotPaused) MapSpecificCommander.onFrame()
        if ( ! Keyboard.GameCommands.isBotPaused) UmsMode.onFrame()
        With.gathering.updateAccelerantPixels()
        With.lambdas.onFrame()
        if ( ! Keyboard.GameCommands.isBotPaused) With.tasks.run(With.performance.msBeforeTarget)
        With.efficiency.update()
        With.storyteller.onFrame()
        Debugging.CherryVis.CherryVis.update()
        // Ladder map diagnostics; meaningless (and baseless-crashy) on UMS maps.
        if ( ! With.umsMode) WriteMapCSV()
        With.performance.endFrame()
        With.camera.onRun(24000)
        With.visualization.onRun(24000)
      })
      // If we don't initialize static units on frame 0 we're in trouble
      tryCatch(() => if (With.frame == 0 && With.units.neutral.isEmpty) { With.units.onFrame() })
    })
  }

  override def onUnitDestroy(unit: bwapi.Unit): Unit = {
    JBWEBWrapper.onUnitDestroy(unit)
    tryCatch(() => With.units.onUnitDestroy(unit))
  }

  override def onUnitComplete(unit: bwapi.Unit): Unit = {}
  override def onUnitDiscover(unit: bwapi.Unit): Unit = {
    JBWEBWrapper.onUnitDiscover(unit)
  }
  override def onUnitHide(unit: bwapi.Unit): Unit = {}

  override def onUnitMorph(unit: bwapi.Unit): Unit = {
    JBWEBWrapper.onUnitMorph(unit)
  }

  override def onUnitRenegade(unit: bwapi.Unit): Unit = {
    tryCatch(() => With.units.onUnitRenegade(unit))
  }

  private var enginePauseNoticed = false

  /**
   * Atlantis parity, generalized: a paused engine with no onFrame flow looks
   * exactly like a dead bot, and Wine can pause the game on focus loss at any
   * time. Lift any pause we did not order ourselves; never fight our own
   * toggle (botPaused tracks exactly the pauses we issued).
   * Edge-triggered: one resume per fresh pause, so a game that insists on
   * staying paused is not spammed every frame.
   */
  private def unpauseIfPaused(): Unit = {
    try {
      val foreignPause = With.game != null && With.game.isPaused && ! Keyboard.GameCommands.isBotPaused
      if (foreignPause && ! enginePauseNoticed) {
        With.game.resumeGame()
        With.logger.debug("Game was paused by something else - sent ResumeGame")
      }
      enginePauseNoticed = foreignPause
    } catch { case exception: Exception => With.logger.quietlyOnException(exception) }
  }

  override def onEnd(isWinner: Boolean): Unit = {
    tryCatch(() => {
      With.logger.debug(f"Game ended in ${?(isWinner, "victory", "defeat")}")
      With.history.onEnd(isWinner)
      With.gathering.onEnd()
      With.storyteller.onEnd()
      With.simulation.onEnd()
      With.manners.onEnd(isWinner)
      // Atlantis GameSummary parity: the run ends with a score box on stdout
      // (the client log tees it to the terminal), not just the launcher line.
      System.out.println(Lifecycle.GameSummary.box(
        isWinner,
        Information.Battles.KillLedger.killed,
        Information.Battles.KillLedger.lost))
      System.out.println(Lifecycle.GameSummary.details(
        With.frame,
        Information.Battles.KillLedger.killed,
        Information.Battles.KillLedger.lost,
        Information.Battles.KillLedger.killedResources,
        Information.Battles.KillLedger.lostResources))
      Debugging.CherryVis.CherryVis.finish(isWinner)
      // Tuning score row (fixed-name + dated copy) when a strategy is active.
      Tuning.Tuning.finish(isWinner)
      // Keyed error counts next to the human log (no-op when nothing failed).
      Debugging.ErrorLedger.finish()
      // Terminate the combat visualizer if we launched it at start
      try { if (With.configuration != null && With.configuration.debugging && With.configuration.simviz) Debugging.CombatVisIO.terminateVisualizer() } catch { case exception: Exception => With.logger.quietlyOnException(exception) }
      With.logger.flush()
    })
    // Single-session Wine runs: the end of the match is the end of the
    // process tree (Atlantis parity). The supervisor waits for this JVM to
    // die and then tears the session down; without it the script sits in
    // `while kill -0 CLIENT_PID` until Escape. Ladder/tournaments keep
    // running: the next match reuses this process.
    // Guarded by frame count: an onEnd delivered without a game ever running
    // (stale score screen on attach) must not kill a fresh session.
    if (Lifecycle.Launch.GameLauncherFactory.isWineClientJvm() && With.frame > 120) {
      With.logger.debug(f"Exiting after game end at frame ${With.frame}")
      With.logger.flush()
      System.exit(0)
    }
  }
  
  override def onSendText(text: String): Unit = {
    tryCatch(() => KeyboardCommands.onSendText(text))
  }

  override def onReceiveText(player: Player, text: String): Unit = {
    tryCatch(() => KeyboardCommands.onReceiveText(text))
  }

  private def tryCatch(lambda: () => Unit): Unit = {
    try {
      lambda()
    } catch { case exception: Exception =>
      System.err.println("[launcher] Exception on frame " + With.frame + ": " + exception)
      exception.printStackTrace()
      With.logger.onException(exception)
    }
  }
}
