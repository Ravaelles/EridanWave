package Micro.Actions.Combat.Spells

import Mathematics.Maff

/**
 * Shared spell-value multipliers (pure numbers, no game access): every
 * valueTarget builds the same chain - confidence, player sign, unit value,
 * danger, battery - with per-spell knobs for the sign, the value and the
 * battery scale. Suite-pinned here so the algebra cannot drift per spell.
 */
object SpellValue {

  /** Confident casters pay full price for doubt: the less sure the caster, the more the hit must be worth. */
  def confidence(confidence11: Double): Double =
    Math.max(1.0, Maff.nanToOne(1 / (1 + confidence11)))

  /** Danger pay: casting while threatened must be worth the caster's own hull. */
  def danger(threatened: Boolean, maxTotalHealth: Double, totalHealth: Double): Double =
    1.0 + 0.5 * (if (threatened) maxTotalHealth / math.max(1.0, totalHealth) else 0.0)

  /** Crowd factor: a spell that can only catch part of a pack scales down. */
  def crowd(targetCount: Int): Double =
    Math.min(1.0, targetCount / 3.0)

  def of(unitValue: Double, playerSign: Double, confidence: Double, danger: Double, battery: Double): Double =
    unitValue * playerSign * confidence * danger * battery
}
