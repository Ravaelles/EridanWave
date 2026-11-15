package Micro.Actions.Protoss.Zealot

/**
 * Zealot-line fallback claim (was CombatTechniques' inline row): blades
 * and Archons do not stand inside a speedling surround. Pure flags in,
 * verdict out; the game reads stay at the call site.
 */
object ZealotFallback {

  def outnumberedBySpeedlings(isBladeLine: Boolean, hasSpeedlingInRange: Boolean): Boolean =
    isBladeLine && hasSpeedlingInRange
}
