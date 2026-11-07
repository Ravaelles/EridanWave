package Lifecycle.Keyboard

import Lifecycle.With

/** Scala facade for global keyboard shortcuts. All game access stays here;
  * Java action classes are one-line delegates, so there is exactly one place
  * where shortcut behavior is defined.
  */
object GameCommands {

  val speedLadder: Vector[(Int, Int)] = Vector(
    (20, 0), (0, 0), (0, 10), (0, 20), (0, 40), (0, 80), (0, 150), (0, 250), (0, 500))

  private var speedIndex  : Int     = 0
  private var guiEnabled  : Boolean = true

  /**
   * Explicit default game speed from --speed= (via PURPLEWAVE_SPEED env).
   * Applied once at game start by MapSpecificCommander, so an explicit
   * choice wins over per-map defaults but later keyboard presets still work.
   */
  lazy val initialSpeed: Option[Int] =
    scala.sys.env.get("PURPLEWAVE_SPEED").flatMap(s =>
      try { Some(s.trim.toInt) } catch { case _: NumberFormatException => None })
      .filter(n => n >= 0 && n <= 1000)

  private def gameAvailable: Boolean = With.game != null

  def setSpeed(speed: Int, frameSkip: Int): Unit = {
    if ( ! gameAvailable) return
    With.game.setLocalSpeed(speed)
    With.game.setFrameSkip(frameSkip)
    speedIndex = nearestIndex(speed, frameSkip)
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

  def setPainting(on: Boolean): Unit = {
    With.configuration.visualizeDebug = on
  }

  def slower(): Unit = {
    step(-1)
  }

  def faster(): Unit = {
    step(1)
  }

  def togglePause(): Unit = {
    if ( ! gameAvailable) return
    if (With.game.isPaused) With.game.resumeGame() else With.game.pauseGame()
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

  private def step(direction: Int): Unit = {
    val next = Math.max(0, Math.min(speedLadder.length - 1, speedIndex + direction))
    val (speed, frameSkip) = speedLadder(next)
    setSpeed(speed, frameSkip)
  }

  private def nearestIndex(speed: Int, frameSkip: Int): Int = {
    speedLadder.indices.minBy(i => {
      val (s, f) = speedLadder(i)
      Math.abs(s - speed) * 1000 + Math.abs(f - frameSkip)
    })
  }
}
