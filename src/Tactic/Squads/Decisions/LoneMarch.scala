package Tactic.Squads.Decisions

/**
 * Pure lone-march gate (no game access): with no friendly warrior inside
 * ten tiles the unit never rallies to the pack centroid or walks to a
 * formation slot - there is no line to pose in, so it marches straight
 * at the squad goal instead of milling alone. The caller measures the
 * nearest warrior; this only compares it.
 */
object LoneMarch {

  /** With no friendly warrior inside this radius, in tiles, march alone. */
  val aloneRadiusTiles: Double = 10.0

  def shouldMarchAlone(nearestWarriorTiles: Double): Boolean =
    nearestWarriorTiles > aloneRadiusTiles
}
