package Tuning

/**
 * Tuning strategy interface (OCP: the loop, transport and scoring below only
 * know this trait). Concrete strategies live in Tuning.Strategies, one file
 * per tuned area; adding a new one never touches the machinery.
 * Referenced by name: --tuning-strategy=KiteAwayDuel.
 */
trait TuningStrategy {

  def name: String

  /** Knobs this strategy tunes (thin lambdas over public owner vars). */
  def params: Seq[TuneParam]

  /** Map file the batch plays for this strategy. */
  def mapFile: String

  /** Games per round (duels are noisy - replicate). */
  def gamesPerRound: Int

  /** Wall-clock seconds a single game may take on the tuning engine. */
  def gameTimeoutSeconds: Int

  /** Fitness of one played game for the hall of fame (higher wins). */
  def fitness(win: Boolean, killed: Int, lost: Int, frames: Int): Double
}
