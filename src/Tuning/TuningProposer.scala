package Tuning

/**
 * Gradient-free proposer: the champion (best fitness so far) plus Gaussian
 * noise scaled by radius over each param's range, clamped to bounds. Empty
 * archive is a cold start: no proposal, the batch randomizes exactly like a
 * plain batch. Halve step and radius when the best stops improving (the
 * driver prints BEST= for the schedule).
 */
object TuningProposer {

  def propose(
    strategy: TuningStrategy,
    hallOfFame: Seq[TuningHallOfFame.Entry],
    radius: Double,
    random: java.util.Random): Map[String, Double] = {
    val ranked = TuningHallOfFame.rank(hallOfFame)
    if (ranked.isEmpty) return Map.empty

    val champion = ranked.head.vector
    strategy.params.map(param => {
      val span = param.max - param.min
      val center = champion.getOrElse(param.name, (param.min + param.max) / 2)
      val noisy = center + random.nextGaussian() * radius * span
      val clamped = Math.min(param.max, Math.max(param.min, noisy))
      param.name -> clamped
    }).toMap
  }

  def formatFragment(proposal: Map[String, Double], strategy: TuningStrategy): String = {
    val byName = strategy.params.map(p => p.name -> p).toMap
    proposal.toSeq.map(pair => {
      val isInt = byName.get(pair._1).exists(_.isInt)
      pair._1 + "=" + Tuning.formatValue(pair._2, isInt)
    }).mkString("\n") + "\n"
  }
}

