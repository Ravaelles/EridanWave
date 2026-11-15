package Lifecycle

import Debugging._
import Utilities.?
import bwapi.{DefaultBWListener, Player}

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
    // A game that ended while paused from outside leaves the flag true;
    // the next game would ignore its first foreign pause instead of
    // resuming (the exact bug the flag exists to prevent).
    enginePauseNoticed = false
    timeFrameZero(() => {
      tryCatch(() => {
        With.onStart()
        // Headless runs (wine-run.sh default) have no window to render and
        // no watcher to wait for: kill the GUI and skip frames to the max
        // so games finish as fast as the bot can think. Visible runs and
        // scbw launches (no ERIDANWAVE_HEADLESS in the env) keep defaults.
        // Map-specific pacing in MapSpecificCommander may still override
        // the skip per frame for its micro maps.
        // (Must run after With.onStart: that is what assigns With.game.)
        if (scala.sys.env.get("ERIDANWAVE_HEADLESS").contains("1")) {
          With.game.setFrameSkip(1000)
          Lifecycle.Keyboard.GameCommands.setGui(false)
        }
        // Tuning params (if --tuning-strategy= was passed): randomize or
        // override before the first frame, Atlantis OnGameStarted parity.
        Tuning.Tuning.install()
        // Static per-map pacing and an explicit --speed= live here, not in
        // the first onFrame: the speed setter sleeps for the engine, which
        // is fine before the frame budget starts ticking.
        if (Lifecycle.Keyboard.Keyboard.isActive()) {
          MapSpecificCommander.applyStaticTuning()
          Keyboard.GameCommands.initialSpeed.foreach(Keyboard.GameCommands.setSpeedOnly)
        }
        With.logger.debug(f"[keyboard] ${Lifecycle.Keyboard.Keyboard.status()}")
        // One sweep clears every per-game family: subsystems register
        // themselves (PerGame, like Latches) instead of adding a line here.
        PerGame.resetAll()
        // Every hysteresis latch registers itself; unit ids restart each game,
        // so one sweep clears the whole family (no per-latch line to forget).
        Utilities.Latches.resetAll()
        Debugging.CherryVis.CherryVis.initialize()
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
        Debugging.MapSnapshot.maybeDump()
        Lifecycle.HitCounter.onFrame(With.units.ours)
        Debugging.DeathWatch.update()
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
        With.missionHistory.onFrame()
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
    tryCatch(() => With.units.onUnitDestroy(unit))
  }

  override def onUnitComplete(unit: bwapi.Unit): Unit = {}
  override def onUnitDiscover(unit: bwapi.Unit): Unit = {}

  override def onUnitHide(unit: bwapi.Unit): Unit = {}

  override def onUnitMorph(unit: bwapi.Unit): Unit = {}

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
      EndOfGame.report(isWinner)
    })
    if (SessionExit.afterGame()) {
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
