package Tactic.Missions.evaluation

/**
 * Pure repeat gate (no game access): a mission that ran before and paid
 * nothing must not keep forming. Levels mirror MissionVerdict
 * (Success 2, Unclear 1, Failure 0); below three past runs anything goes
 * (too little data), at three-plus the average must reach Unclear.
 * Rows parse tolerantly: unknown verdicts drop out, extra columns
 * (profit, harass) are ignored - only the level feeds the gate.
 */
object MissionRepeatGate {

  /** Past runs below this count never veto a new launch. */
  val minRuns: Int = 3

  /** Average level at three-plus runs below which the mission stands down. */
  val minAverageLevel: Double = 1.0

  case class PastRun(level: Int)

  private val knownVerdicts: Set[String] = Set(
    MissionVerdict.name(MissionVerdict.Success),
    MissionVerdict.name(MissionVerdict.Unclear),
    MissionVerdict.name(MissionVerdict.Failure))

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
    if (cols.length < 9 || ! knownVerdicts.contains(cols(4))) return None

    Some((cols(3), PastRun(levelOf(cols(4)))))
  }

  /** Past launches per mission, in file order. Pure for the suite. */
  def groupRows(lines: Seq[String]): Map[String, Vector[PastRun]] =
    lines.flatMap(parseRow).groupBy(_._1).map { case (mission, runs) => (mission, runs.map(_._2).toVector) }

  def averageLevel(runs: Seq[PastRun]): Double =
    if (runs.isEmpty) 0.0 else runs.map(_.level).sum.toDouble / runs.size

  def repeatAllowed(runs: Seq[PastRun]): Boolean =
    runs.size < minRuns || averageLevel(runs) >= minAverageLevel
}
