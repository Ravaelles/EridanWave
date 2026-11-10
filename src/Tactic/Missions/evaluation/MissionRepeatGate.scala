package Tactic.Missions.evaluation

/**
 * Pure repeat gate (no game access): a mission that ran before and paid
 * nothing must not keep forming. Levels mirror MissionVerdict
 * (Success 2, Unclear 1, Failure 0); below three past runs anything goes
 * (too little data), at three-plus the average must reach Unclear.
 * Rows parse tolerantly: the harass column arrived later, legacy nine
 * column rows default it to zero.
 */
object MissionRepeatGate {

  /** Past runs below this count never veto a new launch. */
  val minRuns: Int = 3

  /** Average level at three-plus runs below which the mission stands down. */
  val minAverageLevel: Double = 1.0

  case class PastRun(level: Int, profit: Int)

  def levelOf(verdict: String): Int =
    if (verdict == MissionVerdict.name(MissionVerdict.Success)) 2
    else if (verdict == MissionVerdict.name(MissionVerdict.Unclear)) 1
    else 0

  /**
   * Row shape: time,map,enemy,mission,verdict,profit,lifetime,supply,won
   * with an optional tenth harass-frames column. Returns mission plus run.
   */
  def parseRow(line: String): Option[(String, PastRun)] = {
    val cols = line.split(",").map(_.trim)
    if (cols.length < 9) return None

    try {
      Some((cols(3), PastRun(levelOf(cols(4)), cols(5).toInt)))
    } catch { case _: NumberFormatException => None }
  }

  def averageLevel(runs: Seq[PastRun]): Double =
    if (runs.isEmpty) 0.0 else runs.map(_.level).sum.toDouble / runs.size

  def repeatAllowed(runs: Seq[PastRun]): Boolean =
    runs.size < minRuns || averageLevel(runs) >= minAverageLevel
}
