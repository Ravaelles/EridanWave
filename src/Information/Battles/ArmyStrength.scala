package Information.Battles

import Utilities.?

/**
 * Pure global army strength (no game access): 100 means even, higher
 * means stronger. No scouted enemy counts as even. Displayed on the
 * CherryVis board - and below the retreat threshold it breaks off the
 * attack squad, whatever the local sims say.
 */
object ArmyStrength {

  def score(ours: Double, foe: Double): Double =
    if (foe <= 0.0) 100.0 else 100.0 * ours / foe

  /** Below this global score the attack squad breaks off, whatever the
    * local sims say: reinforcing a losing side feeds it piecemeal, and
    * fresh units march back out as the score recovers. */
  val retreatBelowStrength: Double = 130.0

  /** Duels stay sim-driven: only a real force retreats on the score. */
  val retreatMinWarriors: Int = 6

  def retreating(ours: Double, foe: Double): Boolean =
    foe > 0.0 && score(ours, foe) < retreatBelowStrength

  /**
   * Early-mirror parity floor, in percent: below this unit-count ratio
   * the attack breaks off, whatever the skim says. Skim blurs early
   * mirrors (upgrades unresearched, hulls identical), but hulls do not.
   */
  val retreatBelowUnits: Double = 140.0

  /** UMS double fight margin (owner order): micro maps tolerate twice
    * the disadvantage, so the unit floor halves - retreats still fire
    * when truly outmatched, at half the odds. Ladder keeps 140. */
  val retreatBelowUnitsUms: Double = 70.0

  def retreatBelowUnitsFor(umsMode: Boolean): Double =
    ?(umsMode, retreatBelowUnitsUms, retreatBelowUnits)

  /** Unit-count parity (pure numbers): the same ratio as score, but on
   * raw hulls instead of skim strength. */
  def units(oursUnits: Double, foeUnits: Double): Double =
    score(oursUnits, foeUnits)
  /**
   * Expo punish, in mobile score: a discovered Zerg expansion dies to the
   * main force once our mobile strength clearly outscales theirs. Counted
   * on units alone - a static turtle score must not fake the punch, and a
   * static-heavy defender must not hide from it either.
   */
  val expoPunishScore: Double = 140.0

  def shouldPunishExpo(isZerg: Boolean, expoKnown: Boolean, oursUnits: Double, foeUnits: Double): Boolean =
    isZerg && expoKnown && score(oursUnits, foeUnits) >= expoPunishScore
}
