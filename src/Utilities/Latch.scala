package Utilities

import scala.collection.mutable

/**
 * Hysteresis latch (no game access): a per-key boolean that turns on sharply
 * and only turns off past a release margin, holding its side inside the band.
 * Every flapping doctrine gate in the tree is this one state machine - march
 * dressing, lone march, static push, detector standoff, DT evade, shuttle
 * flee - so it lives here instead of being hand-rolled per gate.
 *
 * `fires` turns the latch on this frame, `releases` turns it off; when neither
 * holds, the previous side stands (or `initial` on the first frame in the
 * band). Keys are usually unit ids, which restart every game, so every latch
 * registers itself and is cleared once per game by `Latches.resetAll()`.
 */
final class Latch {

  private val held = new mutable.HashMap[Int, Boolean]()

  def apply(key: Int, fires: Boolean, releases: Boolean, initial: Boolean = false): Boolean = {
    val next =
      if (fires) true
      else if (releases) false
      else held.getOrElse(key, initial)
    held.put(key, next)

    next
  }

  def reset(): Unit = held.clear()

  Latches.register(this)
}

object Latches {

  private val all = mutable.ArrayBuffer[Latch]()

  private[Utilities] def register(latch: Latch): Unit = all += latch

  /** Unit ids restart every game, so stale holds never cross a game boundary. */
  def resetAll(): Unit = all.foreach(_.reset())
}
