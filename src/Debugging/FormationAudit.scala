package Debugging

import Lifecycle.With

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Temporary formation harness: while `formationAudit` is armed (CLI
 * --formation-audit, off by default), every leaf behavior label is tallied
 * per frame and every rising edge of breakFormationToAttack with a live
 * target is logged with the range band the unit left its slot from. At game
 * end the tally and the per-class break distances (n, mean, stddev, min, max)
 * land in bwapi-data/write/formation-audit.txt for scripts/formation-audit.py.
 * Zero cost when disarmed: one boolean read per hook.
 */
object FormationAudit {

  final case class BreakRow(frame: Int, unitId: Int, ownClass: String, targetClass: String, distTiles: Double)

  private val tallyCounts = new mutable.HashMap[String, Int]
  private val breaks      = new ArrayBuffer[BreakRow]
  private val lastBroken  = new mutable.HashMap[Int, Boolean]

  def reset(): Unit = {
    tallyCounts.clear()
    breaks.clear()
    lastBroken.clear()
  }

  def tallySnapshot: Map[String, Int] = tallyCounts.toMap

  def breakRows: Seq[BreakRow] = breaks.toSeq

  def enabled: Boolean =
    With.configuration != null && With.configuration.formationAudit

  def tally(label: String): Unit = {
    tallyCounts(label) = tallyCounts.getOrElse(label, 0) + 1
  }

  /**
   * Rising edge of breakFormationToAttack with a live target: the frame the
   * unit leaves its slot and starts fighting, with the range band it left
   * from. A unit already broken on its first observed frame logs nothing -
   * no transition was observed.
   */
  def noteBreakState(
    frame       : Int,
    unitId      : Int,
    ownClass    : String,
    broken      : Boolean,
    distTiles   : Option[Double],
    targetClass : Option[String]): Unit = {
    val was = lastBroken.getOrElse(unitId, false)
    lastBroken(unitId) = broken
    if (broken && ! was) {
      for (d <- distTiles; t <- targetClass) breaks += BreakRow(frame, unitId, ownClass, t, d)
    }
  }

  def finish(mapName: String, frames: Int): Unit = {
    if ( ! enabled) return

    val tallyLines = tallyCounts.toSeq.sortBy(-_._2).map({ case (label, n) => f"$n%8d  $label" })
    val byClass = FormationAuditStats.byClass(breaks.toSeq).toSeq.sortBy(-_._2.n)
    val classLines = byClass.map({ case (cls, s) =>
      f"$cls%-28s n=${s.n}%4d mean=${s.mean}%5.2f std=${s.stddev}%5.2f min=${s.min}%5.2f max=${s.max}%5.2f" })
    val rowLines = breaks.map(r => f"${r.frame}%7d ${r.unitId}%5d ${r.ownClass}%-28s ${r.targetClass}%-28s ${r.distTiles}%.2f").toSeq
    val text =
      f"# FormationAudit map=$mapName frames=$frames\n" +
      f"[behaviors] ${tallyCounts.values.sum} leaf decisions\n" +
      tallyLines.mkString("\n") + "\n" +
      f"[breaks-by-class] ${breaks.length} slot leaves\n" +
      classLines.mkString("\n") + "\n" +
      "[break-rows] frame unit own target distTiles\n" +
      rowLines.mkString("\n") + "\n"
    With.bwapiData.writeToFile("formation-audit.txt", text)
    With.logger.debug(f"FormationAudit: ${tallyCounts.size} behaviors, ${breaks.length} breaks -> formation-audit.txt")
  }
}
