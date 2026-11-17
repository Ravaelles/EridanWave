package Micro.Actions.Protoss.Dragoon

import Information.Battles.{ApplyEarlyGamePressurePenalty, ArmyStrength}

/**
 * Early PvP retreat (pure numbers, no game access): an attacking handful
 * of Dragoons facing enemy Dragoons breaks off to the middle choke while
 * outnumbered instead of trading into a superior mirror. Below five of
 * ours the pack cannot focus down a ready line; at parity or better it
 * fights. The caller feeds global army counts, not the local duel - the
 * point is the match, not the skirmish.
 */
object DragoonPvpRetreat {

  /** Below this many of our Dragoons the mirror is not taken. */
  val maxDragoons: Int = 5

  def shouldRetreat(
    isDragoon: Boolean,
    enemyIsProtoss: Boolean,
    oursDragoons: Int,
    enemyHasDragoons: Boolean,
    usUnits: Double,
    foeUnits: Double): Boolean =
    isDragoon &&
      enemyIsProtoss &&
      oursDragoons < maxDragoons &&
      enemyHasDragoons &&
      foeUnits > 0 &&
      ArmyStrength.units(
        ApplyEarlyGamePressurePenalty.adjust(usUnits, usUnits.toInt, enemyIsProtoss, enemyHasDragoons),
        foeUnits) < ArmyStrength.retreatBelowUnits
}
