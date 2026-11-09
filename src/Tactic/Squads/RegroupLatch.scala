package Tactic.Squads

/**
 * Sim-gated regroup (Stardust: Moving | Attacking | Regrouping switched only
 * through a sim verdict). The per-frame fight vote flaps when the simulation
 * hovers near its target, swinging destinations, formations and targets for
 * the whole squad every few frames. This latch switches stance only on a
 * clear sim verdict with asymmetric thresholds: regroup once clearly losing,
 * re-engage only once clearly winning. The deadband between them kills the
 * flap. Missing sim data never flips the stance.
 *
 * Pure decisions on numbers so they stay unit-testable without a game;
 * SquadAttack supplies the game-aware seam (per-unit sim verdicts).
 */
object RegroupLatch {

  sealed trait Stance

  case object Engaged    extends Stance

  case object Regrouping extends Stance

  /** Regroup once fewer than this fraction of sim-judged units wants to fight. */
  val startRegroupBelow: Double = 0.35

  /** Re-engage only once more than this fraction of sim-judged units wants to fight. */
  val stopRegroupAbove: Double = 0.65

  /** Ignore the verdict unless this fraction of the squad carries one. */
  val minimumCoverage: Double = 0.5

  def nextStance(current: Stance, fightingFraction: Double, coverageFraction: Double): Stance = {
    if (coverageFraction < minimumCoverage) return current

    current match {
      case Engaged    => if (fightingFraction < startRegroupBelow) Regrouping else Engaged
      case Regrouping => if (fightingFraction > stopRegroupAbove) Engaged else Regrouping
    }
  }
}


