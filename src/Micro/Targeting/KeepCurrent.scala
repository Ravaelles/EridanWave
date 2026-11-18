package Micro.Targeting

/**
 * Keep-current stickiness for the target ladder (Reaver_v_Sunkens):
 * the ladder re-picks every frame for order-less fighters, so two
 * near-equal targets trade the pick on movement-mill noise and the
 * whole line splits its damage instead of finishing one enemy.
 * A live current pick keeps its lead through the noise; a clearly
 * better challenger still takes it the same frame.
 *
 * Scope is deliberately narrow: this only breaks ties inside one
 * stratum's ranking. Higher strata, doctrines and explicit orders
 * outrank it by construction - they never reach the comparison.
 * Corpses and illegal targets never keep: legality gates first in
 * the ladder, this only combines two legal scores.
 *
 * Pure numbers for the suite; the ladder feeds the two scores.
 */
object KeepCurrent {

  /**
   * Stickiness margin, in score-tiles: frame-to-frame mill moves scores
   * by ~1 tile through the range term, while real doctrine gaps start
   * at 8 (colony urgency) and 12 (combat bonus), so 2.0 damps the mill
   * without freezing genuine switches.
   */
  val bonusTiles: Double = 2.0

  /** The current pick survives unless a challenger beats it past the margin. */
  def keepsTarget(currentScore: Double, bestChallengerScore: Double): Boolean =
    currentScore + bonusTiles >= bestChallengerScore
}
