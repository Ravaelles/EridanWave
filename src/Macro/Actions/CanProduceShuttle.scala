package Macro.Actions

/**
 * Shuttle production veto (no game access): one home for every "should we
 * even queue a Shuttle" question. Gas goes to Observers first (detection
 * beats ferrying), and vs Protoss the Dragoon line comes first - a Shuttle
 * before the line stands is a donation, not a threat.
 *
 * Deliberately NOT here (different concepts, stay with their owners):
 * - how many (one Shuttle per two Reavers lives with the pump counts);
 * - shuttle-first ordering (a gameplan strategy flag);
 * - harass timing on Shuttle counts (a harass doctrine, not production).
 */
object CanProduceShuttle {

  /** Below this many Dragoons vs Protoss no Shuttle queues, period. */
  val minDragoonsVsProtoss: Int = 7

  /** Below this many Dragoons vs Protoss the strength gate applies. */
  val dragoonsForStrengthGate: Int = 12

  /**
   * Units-strength score (ArmyStrength.score, 100 = parity) at or below
   * which a short Dragoon line keeps building Dragoons instead of a
   * Shuttle - vs Protoss only. Above it we are crushing hard enough to
   * afford the ferry. Unknown enemies score 100, so they hold the veto.
   */
  val minUnitsScoreVsProtoss: Double = 250.0

  def canProduce(
    observerDemanded: Boolean,
    enemyIsProtoss: Boolean,
    dragoons: Int,
    unitsScore: Double): Boolean = {
    if (observerDemanded) return false

    if (enemyIsProtoss && (dragoons < minDragoonsVsProtoss ||
      (dragoons < dragoonsForStrengthGate && unitsScore <= minUnitsScoreVsProtoss))) return false

    true
  }
}
