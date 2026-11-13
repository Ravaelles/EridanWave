package Debugging

import Lifecycle.With

import scala.collection.mutable
import scala.io.Source

/**
 * Dynamic per-unit decision probe for games without a CherryVis trace
 * (UMS micro maps): the change-only decision channel rides the debug log
 * instead, one PROBE line per change, so micro duels stay readable
 * post-game without log spam. Zero cost and zero output unless a key is
 * armed: drop key names (one per line) into bwapi-data/write/probe.txt
 * mid-game and the next refresh picks them up, no restart needed.
 *
 * One line to add a probe point: Probe.note("Act", unit.id, message).
 * Read them back with scripts/probe.py against the debug log.
 */
object Probe {

  private var enabledKeys = Set.empty[String]
  private var lastRefreshFrame = Int.MinValue
  private val lastByUnitKey = new mutable.HashMap[(String, Int), String]()

  private def probeFile: String = With.bwapiData.write + "/probe.txt"

  def on(key: String): Boolean = {
    refresh()
    enabledKeys.contains(key)
  }

  def note(key: String, unitId: Int, message: => String): Unit = {
    if ( ! on(key)) return

    // By-name message: an unarmed key never pays for its interpolation.
    val text = message
    val seen = (key, unitId)
    if (lastByUnitKey.get(seen).contains(text)) return

    lastByUnitKey(seen) = text
    With.logger.debug(f"PROBE $key ${With.frame} $unitId $text")
  }

  def reset(): Unit = {
    enabledKeys = Set.empty
    lastRefreshFrame = Int.MinValue
    lastByUnitKey.clear()
  }

  private def refresh(): Unit = {
    if (With.frame - lastRefreshFrame < 24 && lastRefreshFrame != Int.MinValue) return

    lastRefreshFrame = With.frame
    try {
      val file = new java.io.File(probeFile)
      enabledKeys = if (file.isFile) Source.fromFile(file).getLines().map(_.trim).filter(_.nonEmpty).toSet else Set.empty
    } catch { case _: Exception => enabledKeys = Set.empty }
  }
}
