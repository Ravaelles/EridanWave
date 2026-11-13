package Micro.Actions.Protoss.Dragoon

/**
 * Brave Dragoon vs Marines (no game access): early against Terran bio a
 * ready shielded Dragoon trades instead of retreating - kiting lone
 * Marines wastes the Dragoon's range while the sim dithers. The caller
 * feeds shields, cooldown, supply, score and the all-Marines read.
 */
object DragoonBrave {

  /** Stand and fight above this many shields. */
  val braveShields: Int = 10

  /** Stand and fight with cooldown at or below this. */
  val braveCooldown: Int = 6

  /** Early game while supply stays at or below this. */
  val earlySupply: Int = 70

  /** ...or while our global score crushes at or above this. */
  val braveScore: Double = 400.0

  def earlyGame(supplyUsed200: Int, score: Double): Boolean =
    supplyUsed200 <= earlySupply || score >= braveScore

  def applies(
    dragoonVsTerran : Boolean,
    shields         : Int,
    cooldownLeft    : Int,
    supplyUsed200   : Int,
    score           : Double,
    foesMarines     : Boolean): Boolean =
    dragoonVsTerran &&
      shields >= braveShields &&
      cooldownLeft <= braveCooldown &&
      earlyGame(supplyUsed200, score) &&
      foesMarines
}
