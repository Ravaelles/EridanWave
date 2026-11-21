package Debugging

import Lifecycle.PerGame
import Lifecycle.With

import java.io.BufferedWriter
import java.io.FileWriter

import scala.io.Source

/**
 * Full behavior-sequence recorder for traceless UMS maps (no game
 * access beyond the log path): while the Probe logs one line per
 * CHANGE (repeats collapse, so alternations hide), this writes every
 * unit's decision chain every frame to a temp file, so a cycle
 * detector can count how often two behaviors actually alternate.
 * Armed by bwapi-data/write/behavior.txt (unit ids, one per line, or
 * * for all); zero cost and zero output without it. The file is temp
 * only (git-ignored, overwritten per game) and never read by the bot -
 * read it back with scripts/detect-cycles.py.
 */
object BehaviorLog extends PerGame {

  private var armedUnits = Set.empty[String]
  private var lastRefreshFrame = Int.MinValue
  private var writer: Option[BufferedWriter] = None

  private def logFile: String = With.bwapiData.write + "/behavior.log"

  private def armFile: String = With.bwapiData.write + "/behavior.txt"

  def note(unitId: Int, chain: String): Unit = {
    if ( ! isArmed(unitId)) return

    try {
      val out = writer.getOrElse {
        val fresh = new BufferedWriter(new FileWriter(logFile, false))
        writer = Some(fresh)
        fresh
      }
      out.write(f"${With.frame} $unitId $chain\n")
      if (With.frame % 24 == 0) out.flush()
    } catch { case _: Exception => }
  }

  private def isArmed(unitId: Int): Boolean = {
    refresh()
    armedUnits.contains("*") || armedUnits.contains(unitId.toString)
  }

  private def refresh(): Unit = {
    if (With.frame - lastRefreshFrame < 24 && lastRefreshFrame != Int.MinValue) return

    lastRefreshFrame = With.frame
    try {
      val file = new java.io.File(armFile)
      armedUnits = if (file.isFile) Source.fromFile(file).getLines().map(_.trim).filter(_.nonEmpty).toSet else Set.empty
    } catch { case _: Exception => armedUnits = Set.empty }
  }

  override def reset(): Unit = {
    try { writer.foreach(_.close()) } catch { case _: Exception => }
    writer = None
    armedUnits = Set.empty
    lastRefreshFrame = Int.MinValue
  }
}
