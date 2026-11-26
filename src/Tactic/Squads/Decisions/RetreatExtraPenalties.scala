package Tactic.Squads.Decisions

import Lifecycle.With
import Mathematics.Maff

/**
 * Extra retreat penalties (one home for every odds adjustment that
 * makes retreat easier): the base gate (RetreatAllowed) only knows
 * strong-vs-weak, so every situational discount lives here, beside
 * the knobs and the suite rows that pin them. Triggers (NaturalChoke
 * legs) and vetoes (NoRetreat gates) answer different questions and
 * stay where they are - this file only ever multiplies assessed
 * strength down, never votes retreat by itself.
 *
 * Discipline content moved verbatim from RetreatDiscipline (same
 * doctrine, one address): once the cluster breaks off, it stays
 * broken off - a withdrawal that re-engages the moment a sim
 * flickers back to optimistic feeds the line piecemeal.
 */
object RetreatExtraPenalties {

  /** Withdrawal floor, in frames: nothing re-engages sooner than this. */
  val minWithdrawFrames: Int = 48

  /**
   * Advantage multiplier by frames since the withdrawal started: fresh
   * wounds read weakest, old ones read whole again.
   */
  def advantageMultiplier(framesSinceWithdraw: Int): Double =
    if (framesSinceWithdraw < 24) 0.6
    else if (framesSinceWithdraw < 48) 0.7
    else if (framesSinceWithdraw < 72) 0.8
    else if (framesSinceWithdraw <= 100) 0.85
    else if (framesSinceWithdraw <= 180) 0.9
    else 1.0

  /**
   * Withdrawal hold (pure rule for the posture latch): stay withdrawn
   * while the floor has not elapsed; after that the penalized advantage
   * decides, exactly like any other frame.
   */
  def shouldHoldWithdraw(framesSinceWithdraw: Int): Boolean =
    framesSinceWithdraw < minWithdrawFrames

  /** Weak expedition bar: below this many warriors the force is thin. */
  val weakExpeditionWarriors: Int = 12

  /**
   * Far expedition line, in ground tiles from our main: past this the
   * alpha centroid fights deep in enemy territory, not on our ground.
   */
  val farExpeditionTiles: Double = 45.0

  /**
   * Expedition score share: a weak force this far out counts only this
   * fraction of its skim (owner order, BetaStar grind: a thin line
   * that stands (Reposition) instead of walking home donates itself).
   * Retreats sooner, fights less - never the reverse.
   */
  val expeditionScoreShare: Double = 0.75

  /**
   * Penalized strength (pure numbers for the suite): a thin force deep
   * out multiplies its skim down, everyone else reads whole. Applied
   * by every retreat reader (technique arm, march arm, posture vote)
   * to the same score, so the trio agrees instead of drifting.
   */
  def penalizedScore(unitsScore: Double, combatWarriors: Int, alphaDistTiles: Double): Double =
    if (combatWarriors < weakExpeditionWarriors && alphaDistTiles >= farExpeditionTiles)
      unitsScore * expeditionScoreShare
    else unitsScore

  /**
   * Game reads for the expedition gate: live warrior count and the
   * alpha centroid's ground distance from our main, in tiles. One
   * seam beside the kernel, read by every arm. Baseless maps (UMS
   * duels) have no home to measure from - they read zero distance,
   * so the gate never fires there; small maps never reach the far
   * bar anyway.
   */
  def expeditionReads(): (Int, Double) = {
    val warriors = With.units.ours.count(u => u.alive && u.unitClass.isWarrior)
    if (With.geography.ourBases.isEmpty) return (warriors, 0.0)

    (warriors,
      With.tactics.attackSquad.centroidGround
        .groundPixels(With.geography.ourMain.heart.center) * Maff.inv32)
  }

  /**
   * Penalized score for one arm (no game beyond the two reads above):
   * reads the expedition and applies the discount in one call, so
   * every arm votes on the same vintage instead of re-deriving it.
   */
  def expeditionScore(unitsScore: Double): Double = {
    val (warriors, distTiles) = expeditionReads()

    penalizedScore(unitsScore, warriors, distTiles)
  }
}
