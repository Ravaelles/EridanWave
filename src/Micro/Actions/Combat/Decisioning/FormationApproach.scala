package Micro.Actions.Combat.Decisioning

/**
 * FormationApproachEnemy: governor for closing the distance as a dressed line
 * instead of trickling in piecemeal.
 *
 * Far from the enemy the line marches freely. Inside the dress band it stops
 * racing: each unit compares its own distance-to-contact against the cluster
 * mean, steps back a touch when protruding past tolerance, marches on when
 * lagging past tolerance, and otherwise holds ground. Once the line is even
 * (low standard deviation) the whole line closes together, cautiously or
 * decisively depending on the enemy race.
 *
 * All distances in tiles (1 tile = 32 px, as in Atlantis). Pure decisions on
 * numbers so they stay unit-testable without a game; Combat supplies the
 * game-aware seam (distances, stats, posture).
 */
object FormationApproach {

  /** How the even line closes: fast and decisive vs Terran, cautious vs Zerg/Protoss. */
  sealed trait ApproachPosture

  case object Cautious extends ApproachPosture

  case object Decisive extends ApproachPosture

  /** Line shape: mean/stddev of member distances beyond their own firing range, in tiles. */
  case class LineStats(meanTiles: Double, stddevTiles: Double, count: Int)

  /** Enemy further than this beyond the line means free march, no dressing. */
  val dressBandTiles: Double = 5.0

  /** Protrusion/recession within this of the mean means hold ground. */
  val lineToleranceTiles: Double = 0.4

  /** Single correction step back, issued at most every correctionCadence frames (hold between). */
  val correctionStepTiles: Double = 0.1
  val correctionCadence: Int = 12

  /** A line this even closes together instead of dressing individually. */
  val evenLineStddevTiles: Double = 0.5

  sealed trait ApproachOrder

  case object FreeMarch extends ApproachOrder

  case object HoldLine extends ApproachOrder

  case object StepBack extends ApproachOrder

  case object StepForward extends ApproachOrder

  case object CloseCautious extends ApproachOrder

  case object BreakThrough extends ApproachOrder

  def lineStats(beyondTiles: Iterable[Double]): Option[LineStats] = {
    val xs = beyondTiles.toSeq
    if (xs.isEmpty) None
    else {
      val mean = xs.sum / xs.size
      val variance = xs.view.map(x => (x - mean) * (x - mean)).sum / xs.size
      Some(LineStats(mean, Math.sqrt(variance), xs.size))
    }
  }

  def decide(ownBeyondTiles: Double, stats: Option[LineStats], posture: ApproachPosture): ApproachOrder = {
    stats match {
      case None => FreeMarch
      case Some(line) =>
        if (line.meanTiles > dressBandTiles) FreeMarch
        else if (line.stddevTiles < evenLineStddevTiles) posture match {
          case Cautious => CloseCautious
          case Decisive => BreakThrough
        } else {
          val deviation = ownBeyondTiles - line.meanTiles
          if (deviation > lineToleranceTiles) StepBack
          else if (deviation < -lineToleranceTiles) StepForward
          else HoldLine
        }
    }
  }
}

