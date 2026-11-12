package Debugging

import Debugging.FormationAudit.BreakRow

/**
 * Pure break-distance math for the formation harness: per-class n, mean,
 * population stddev, min and max over the slot-leave range bands. The game
 * reads stay in FormationAudit; this only folds numbers, so the table can
 * be argued with in a test.
 */
object FormationAuditStats {

  final case class BreakStats(n: Int, mean: Double, stddev: Double, min: Double, max: Double)

  def summarize(distances: Seq[Double]): Option[BreakStats] = {
    if (distances.isEmpty) return None

    val n = distances.size
    val mean = distances.sum / n
    val variance = distances.view.map(d => (d - mean) * (d - mean)).sum / n

    Some(BreakStats(n, mean, math.sqrt(variance), distances.min, distances.max))
  }

  def byClass(rows: Seq[BreakRow]): Map[String, BreakStats] =
    rows.groupBy(_.ownClass).flatMap({ case (cls, rs) => summarize(rs.map(_.distTiles)).map(cls -> _) })
}
