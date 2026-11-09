package Lifecycle.Keyboard

import Lifecycle.With

/** Scala facade for global keyboard shortcuts. All game access stays here;
  * Java action classes are one-line delegates, so there is exactly one place
  * where shortcut behavior is defined.
  */
object GameCommands {

  val speedLadder: Vector[(Int, Int)] = Vector(
    (20, 0), (0, 0), (0, 10), (0, 20), (0, 40), (0, 80), (0, 150), (0, 250), (0, 500))

  @volatile private var speedIndex  : Int     = 0
  private var guiEnabled  : Boolean = true
  @volatile private var botPaused   : Boolean = false

  /**
   * Explicit default game speed from --speed= (via ERIDANWAVE_SPEED env;
   * legacy PURPLEWAVE_SPEED is still accepted).
   * Applied once at game start by MapSpecificCommander, so an explicit
   * choice wins over per-map defaults but later keyboard presets still work.
   */
  lazy val initialSpeed: Option[Int] =
    scala.sys.env.get("ERIDANWAVE_SPEED").orElse(scala.sys.env.get("PURPLEWAVE_SPEED")).flatMap(s =>
      try { Some(s.trim.toInt) } catch { case _: NumberFormatException => None })
      .filter(n => n >= 0 && n <= 1000)

  private def gameAvailable: Boolean = With.game != null

  /**
   * StarCraft needs a speed change bracketed by a short delay, otherwise the
   * settings silently don't apply (measured in Atlantis - hence the constant).
   * The bracket never touches the user-visible pause: it only spaces out the
   * BWAPI calls on the calling thread.
   */
  private val SpeedDelayMs = 25
  private def applySpeedAndFrameSkip(speedMs: Int, skip: Int): Unit = {
    if ( ! gameAvailable) return

    try { Thread.sleep(SpeedDelayMs) } catch { case _: InterruptedException => }
    With.game.setLocalSpeed(speedMs)
    With.game.setFrameSkip(skip)
    With.game.setFrameSkip(skip) // Twice, like Atlantis: a single call is unreliable
    With.game.setGUI(skip <= 150) // Huge skips turn the GUI off, like Atlantis
    try { Thread.sleep(SpeedDelayMs) } catch { case _: InterruptedException => }
    speedIndex = nearestIndex(speedMs, skip)
  }

  def setSpeed(speed: Int, frameSkip: Int): Unit = {
    applySpeedAndFrameSkip(speed, frameSkip)
  }

  /** Change speed, keep the current frame skip. */
  def setSpeedOnly(speed: Int): Unit = {
    setSpeed(speed, frameSkip)
  }

  /** Change frame skip, keep the current speed. */
  def setFrameSkipOnly(frameSkip: Int): Unit = {
    val (speed, _) = speedLadder(speedIndex)
    setSpeed(speed, frameSkip)
  }

  def frameSkip: Int = speedLadder(speedIndex)._2

  def speedMs: Int = speedLadder(speedIndex)._1

  def setPainting(on: Boolean): Unit = {
    With.configuration.visualizeDebug = on
  }

  /**
   * One step slower/faster in Atlantis units: 15ms of game speed, collapsing
   * to a stop (0) at the fast end - and bumping the frame skip instead once
   * already stopped. Same collapse rules as Atlantis changeSpeed/changeSpeedTo.
   */
  def slower(): Unit = {
    changeSpeedTo(if (speedMs == 0) 1 else speedMs + 15)
  }

  def faster(): Unit = {
    changeSpeedTo(if (speedMs > 1) 1 else 0)
  }

  private def changeSpeedTo(requestedMs: Int): Unit = {
    if (requestedMs <= 0) {
      val skip = if (speedMs == 0) frameSkip + 5 else frameSkip
      applySpeedAndFrameSkip(0, skip)
    } else {
      applySpeedAndFrameSkip(requestedMs, 0)
    }
  }

  def isBotPaused: Boolean = botPaused

  /**
   * True engine pause: the game freezes mid-step and JBWAPI keeps delivering
   * onFrame, so tracking, the overlay and chat commands keep running while
   * decisions and orders stay parked by the guards in EridanWave.onFrame.
   * Silent by design: no chat, no pings. The toggle follows the engine, not
   * an assumed state, so a pause imposed from outside (stale score screen,
   * focus loss) always resolves the same way.
   */
  def togglePause(): Unit = {
    if (With.game == null) return

    if (With.game.isPaused) {
      botPaused = false
      With.game.resumeGame()
    } else {
      botPaused = true
      With.game.pauseGame()
    }
  }

  def toggleGui(): Unit = {
    if ( ! gameAvailable) return

    guiEnabled = ! guiEnabled
    With.game.setGUI(guiEnabled)
  }

  def togglePainting(): Unit = {
    With.configuration.visualizeDebug = ! With.configuration.visualizeDebug
  }

  def toggleCamera(): Unit = {
    With.configuration.camera = ! With.configuration.camera
  }

  def toggleVisualization(): Unit = {
    With.visualization.enabled = ! With.visualization.enabled
  }

  private def nearestIndex(speed: Int, frameSkip: Int): Int = {
    speedLadder.indices.minBy(i => {
      val (s, f) = speedLadder(i)
      Math.abs(s - speed) * 1000 + Math.abs(f - frameSkip)
    })
  }
}

