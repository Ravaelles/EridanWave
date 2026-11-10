package Tactic.Missions.evaluation

/**
 * Pure mission verdict (no game access): profit over cost is success and
 * repeats; a twenty-second harass is success by worker delay alone; dead
 * inside ten seconds is failure. A won game upgrades one level: a tactical
 * suicide that wins the war still counts. Per-mission rules override by
 * subclassing the evaluator, not by editing this table.
 */
object MissionVerdict {

  sealed trait Verdict

  case object Success extends Verdict

  case object Unclear extends Verdict

  case object Failure extends Verdict

  /** Past this lifetime the harass paid for itself in worker delay. */
  val harassLifetimeFrames: Int = 20 * 24

  /** Dead inside this lifetime never paid for itself. */
  val doomedLifetimeFrames: Int = 10 * 24

  /**
   * Worker-harass frames needed for full credit: a minute denying mining
   * succeeds on its own. Below it credit scales linearly, so partial
   * pressure moves the needle instead of flipping a cliff.
   */
  val harassSuccessFrames: Int = 60 * 24

  def evaluate(profitResources: Int, lifetimeFrames: Int, wonGame: Boolean, workerHarassFrames: Int = 0): Verdict = {
    val harassCredit = Math.max(0.0, Math.min(1.0, workerHarassFrames.toDouble / harassSuccessFrames))
    var level =
      if (profitResources > 0) 2.0
      else if (lifetimeFrames >= harassLifetimeFrames) 2.0
      else if (lifetimeFrames <= doomedLifetimeFrames) harassCredit * 2.0
      else 1.0 + harassCredit
    if (wonGame) level += 1.0
    level = Math.min(2.0, Math.max(0.0, level))
    if (level >= 1.5) Success else if (level >= 0.5) Unclear else Failure
  }

  def name(verdict: Verdict): String = verdict match {
    case Success => "Success"
    case Unclear => "Unclear"
    case Failure => "Failure"
  }
}
