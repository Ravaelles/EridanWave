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

  final case class DressRow(
    frame         : Int,
    unitId        : Int,
    ownClass      : String,
    ownBeyondTiles: Double,
    lineMeanTiles : Double,
    lineStddevTiles: Double,
    lineCount     : Int,
    posture       : String,
    order         : String)

  private val tallyCounts = new mutable.HashMap[String, Int]
  private val verdictCounts = new mutable.HashMap[String, Int]
  private val breaks      = new ArrayBuffer[BreakRow]
  private val dresses     = new ArrayBuffer[DressRow]
  private val lastBroken  = new mutable.HashMap[Int, Boolean]

  def reset(): Unit = {
    tallyCounts.clear()
    verdictCounts.clear()
    breaks.clear()
    dresses.clear()
    lastBroken.clear()
  }

  def tallySnapshot: Map[String, Int] = tallyCounts.toMap

  def tallyVerdict(reason: String): Unit = {
    verdictCounts(reason) = verdictCounts.getOrElse(reason, 0) + 1
  }

  def verdictSnapshot: Map[String, Int] = verdictCounts.toMap

  def breakRows: Seq[BreakRow] = breaks.toSeq

  def dressRows: Seq[DressRow] = dresses.toSeq

  /**
   * One dress verdict per eligible unit-frame: where the unit stands
   * relative to its range, the line shape it dresses against, and the order
   * it took. Alternating StepBack/StepForward/HoldLine across frames is the
   * oscillation signature scripts/formation-audit.py ranks per unit.
   */
  def noteDress(
    frame         : Int,
    unitId        : Int,
    ownClass      : String,
    ownBeyondTiles: Double,
    lineMeanTiles : Double,
    lineStddevTiles: Double,
    lineCount     : Int,
    posture       : String,
    order         : String): Unit = {
    dresses += DressRow(frame, unitId, ownClass, ownBeyondTiles, lineMeanTiles, lineStddevTiles, lineCount, posture, order)
  }

  def enabled: Boolean =
    With.configuration != null && With.configuration.formationAudit

  def tally(label: String): Unit = {
    tallyCounts(label) = tallyCounts.getOrElse(label, 0) + 1
  }

  /**
   * Rising edge of breakFormationToAttack with a live target and a formation
   * to leave: the frame the unit leaves its slot and starts fighting, with
   * the range band it left from. A unit already broken on its first observed
   * frame logs nothing - no transition was observed - and a unit with no
   * formation logs nothing either, since there is no slot to leave (frame 0
   * before staffing, or a squadless drifter).
   */
  def noteBreakState(
    frame       : Int,
    unitId      : Int,
    ownClass    : String,
    broken      : Boolean,
    hasFormation: Boolean,
    distTiles   : Option[Double],
    targetClass : Option[String]): Unit = {
    val was = lastBroken.getOrElse(unitId, false)
    lastBroken(unitId) = broken
    if (broken && ! was && hasFormation) {
      for (d <- distTiles; t <- targetClass) breaks += BreakRow(frame, unitId, ownClass, t, d)
    }
  }

  def finish(mapName: String, frames: Int): Unit = {
    if ( ! enabled) return

    val tallyLines = tallyCounts.toSeq.sortBy(-_._2).map({ case (label, n) => f"$n%8d  $label" })
    val verdictLines = verdictCounts.toSeq.sortBy(-_._2).map({ case (label, n) => f"$n%8d  $label" })
    val byClass = FormationAuditStats.byClass(breaks.toSeq).toSeq.sortBy(-_._2.n)
    val classLines = byClass.map({ case (cls, s) =>
      f"$cls%-28s n=${s.n}%4d mean=${s.mean}%5.2f std=${s.stddev}%5.2f min=${s.min}%5.2f max=${s.max}%5.2f" })
    val rowLines = breaks.map(r => f"${r.frame}%7d ${r.unitId}%5d ${r.ownClass}%-28s ${r.targetClass}%-28s ${r.distTiles}%.2f").toSeq
    val dressCounts = dresses.groupBy(_.order).map({ case (order, rs) => (order, rs.size) }).toSeq.sortBy(-_._2)
    val dressCountLines = dressCounts.map({ case (order, n) => f"$n%8d  $order" })
    val dressRowLines = dresses.map(d =>
      f"${d.frame}%7d ${d.unitId}%5d ${d.ownClass}%-16s ${d.ownBeyondTiles}%5.2f ${d.lineMeanTiles}%5.2f ${d.lineStddevTiles}%5.2f ${d.lineCount}%3d ${d.posture}%-10s ${d.order}").toSeq
    val text =
      f"# FormationAudit map=$mapName frames=$frames\n" +
      f"[behaviors] ${tallyCounts.values.sum} leaf decisions\n" +
      tallyLines.mkString("\n") + "\n" +
      f"[verdicts] ${verdictCounts.values.sum} fight votes\n" +
      verdictLines.mkString("\n") + "\n" +
      f"[breaks-by-class] ${breaks.length} slot leaves\n" +
      classLines.mkString("\n") + "\n" +
      "[break-rows] frame unit own target distTiles\n" +
      rowLines.mkString("\n") + "\n" +
      f"[dress-orders] ${dresses.length} verdicts\n" +
      dressCountLines.mkString("\n") + "\n" +
      "[dress-rows] frame unit own beyond mean std count posture order\n" +
      dressRowLines.mkString("\n") + "\n"
    With.bwapiData.writeToFile("formation-audit.txt", text)
    With.logger.debug(f"FormationAudit: ${tallyCounts.size} behaviors, ${verdictCounts.size} verdicts, ${breaks.length} breaks, ${dresses.length} dress verdicts -> formation-audit.txt")
  }
}
