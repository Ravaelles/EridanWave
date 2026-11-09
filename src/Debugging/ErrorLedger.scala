package Debugging

import scala.collection.mutable

/**
 * Keyed error ledger: every distinct failure gets a stable greppable key and
 * a priority, counted instead of flooding the log every frame.
 *
 * A key looks like `exception.NullPointerException.at.GeographyCache:22` -
 * it leads straight to the call site, unlike a 50k-line wall of repeated
 * traces. Counts, first/last frames and a few sample messages go to
 * errors.json at game end (next to the CherryVis trace); the human log
 * keeps its full lines. Query token-cheap with
 * scripts/summarize-errors.py (one line per key).
 *
 * Pure core (grouping, filtering, rendering) stays testable without a game;
 * only reset/finish touch With.
 */
object ErrorLedger {

  object Priority {
    val High    = "high"
    val Medium  = "medium"
    val Low     = "low"
    private val rank = Map(High -> 3, Medium -> 2, Low -> 1)
    def atLeast(priority: String, minimum: String): Boolean =
      rank.getOrElse(priority, 0) >= rank.getOrElse(minimum, 0)
  }

  case class Note(key: String, priority: String, count: Int, firstFrame: Int, lastFrame: Int, samples: Vector[String])

  private case class Entry(priority: String, var count: Int, firstFrame: Int, var lastFrame: Int, samples: mutable.ArrayBuffer[String])

  private val entries = new mutable.LinkedHashMap[String, Entry]

  val maxSamples: Int = 3

  def note(key: String, priority: String, message: => String): Int = {
    entries.get(key) match {
      case Some(entry) =>
        entry.count += 1
        entry.lastFrame = Lifecycle.With.frame
        if (entry.samples.size < maxSamples) entry.samples += message
        entry.count
      case None =>
        entries(key) = Entry(priority, 1, Lifecycle.With.frame, Lifecycle.With.frame, mutable.ArrayBuffer(message))
        1
    }
  }

  /** Full traces stay readable: log one for the first few hits of a key, count the rest. */
  def deservesFullTrace(occurrences: Int): Boolean = occurrences <= 2

  /** Stable key for an exception: class plus top in-repo stack frame. */
  def exceptionKey(exception: Exception): String = {
    val top = exception.getStackTrace.find(e =>
      e.getClassName.startsWith("ProxyBwapi.") || e.getClassName.startsWith("Lifecycle.") ||
      e.getClassName.startsWith("Tactic.") || e.getClassName.startsWith("Micro.") ||
      e.getClassName.startsWith("Information.") || e.getClassName.startsWith("Strategery.") ||
      e.getClassName.startsWith("Macro.") || e.getClassName.startsWith("Debugging.") ||
      e.getClassName.startsWith("Planning.")).map(e =>
      e.getClassName.split('.').last + ":" + e.getLineNumber).getOrElse("unknown")
    "exception." + exception.getClass.getSimpleName + ".at." + top
  }

  def reset(): Unit = entries.clear()

  def notes: Seq[Note] = entries.toSeq.map { case (key, entry) =>
    Note(key, entry.priority, entry.count, entry.firstFrame, entry.lastFrame, entry.samples.toVector)
  }

  /** Notes at or above a priority, worst first (priority, then count). */
  def atLeast(minimum: String): Seq[Note] = {
    notes.filter(note => Priority.atLeast(note.priority, minimum)).sortBy(note =>
      (-priorityRank(note.priority), -note.count))
  }

  def toJson: String = {
    notes.map(note =>
      "{\"key\":" + quote(note.key) +
      ",\"priority\":" + quote(note.priority) +
      ",\"count\":" + note.count +
      ",\"firstFrame\":" + note.firstFrame +
      ",\"lastFrame\":" + note.lastFrame +
      ",\"samples\":[" + note.samples.map(quote).mkString(",") + "]}").mkString("[", ",", "]")
  }

  def finish(): Unit = {
    if (entries.isEmpty) return

    try {
      Lifecycle.With.bwapiData.writeToFile("errors.json", toJson)
      System.out.println(f"[errors] ${entries.size} distinct keys, ${entries.values.map(_.count).sum} total notes -> errors.json")
    } catch { case exception: Exception => Lifecycle.With.logger.onException(exception) }
  }

  private def priorityRank(priority: String): Int = priority match {
    case Priority.High => 3
    case Priority.Medium => 2
    case _ => 1
  }

  private def quote(text: String): String = {
    "\"" + text.flatMap {
      case '"' => "\\\""
      case '\\' => "\\\\"
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case c if c < 0x20 => f"\\u${c.toInt}%04x"
      case c => c.toString
    }.mkString + "\""
  }
}

