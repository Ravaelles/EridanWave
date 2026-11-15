package Lifecycle

import scala.collection.mutable

/**
 * Per-game resettable state (one registry for onStart, no per-subsystem
 * line to forget): objects extend this instead of adding their reset to
 * the onStart list by hand. Registration happens once at construction, so
 * a fresh subsystem starts registered; resetAll sweeps the whole family
 * the way Latches.resetAll sweeps the latch family.
 */
trait PerGame {

  PerGame.register(() => reset())

  def reset(): Unit
}

object PerGame {

  private val all = new mutable.ArrayBuffer[() => Unit]()

  private[Lifecycle] def register(reset: () => Unit): Unit = all += reset

  def resetAll(): Unit = all.foreach(_())
}
