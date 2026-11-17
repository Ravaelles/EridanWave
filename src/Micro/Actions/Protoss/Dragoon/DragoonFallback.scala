package Micro.Actions.Protoss.Dragoon

import Micro.Coordination.Pushing.TrafficPriority
import Micro.Coordination.Pushing.TrafficPriorities

/**
 * Dragoon fallback claim (was CombatTechniques' inline row): a Dragoon
 * facing an all-rush pack at firing range falls back instead of standing
 * into the surround - Vultures, speedlots and Zerglings only ever close.
 * Pure numbers in, verdict out; the game reads stay at the call site.
 * No ranged backup, no fallback: a pack of pure melee dies to a brave
 * line (zealots leashed in, dragoons shooting), while retreating hands
 * the rush the base behind it.
 */
object DragoonFallback {

  def facesRush(
    isDragoon: Boolean,
    pushPriority: TrafficPriority,
    hasRangedTarget: Boolean,
    allRushers: Boolean,
    enemyHasRanged: Boolean): Boolean =
    isDragoon &&
      pushPriority < TrafficPriorities.Shove &&
      hasRangedTarget &&
      allRushers &&
      enemyHasRanged
}
