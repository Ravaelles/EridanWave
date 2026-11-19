package Tactic.Squads.Decisions

import Utilities.Latch

/**
 * Pure lone-march gate (no game access): with no friendly warrior inside
 * fifteen tiles the unit never rallies to the pack centroid or walks to a
 * formation slot - there is no line to pose in, so it marches straight
 * at the squad goal instead of milling alone. The caller measures the
 * nearest warrior; this only compares it.
 *
 * The sharp boundary has a latched twin below: the terminus on either
 * side lives hundreds of pixels apart (squad goal vs nearest-ally pixel),
 * so a unit breathing across exactly fifteen tiles jerks its destination
 * every few frames - a live UMS game showed the final jumping +-300px
 * with 44-65% alternation on every fighter. The latch marches alone past
 * 17, rejoins below 13, and holds its side inside the band. Per-unit
 * state, reset per game like the other march watches.
 */
object LoneMarch {

  /** With no friendly warrior inside this radius, in tiles, march alone. */
  val aloneRadiusTiles: Double = 15

  /** Past this distance, in tiles, a packing unit starts marching alone. */
  val aloneEnterTiles: Double = 17

  /** Below this distance, in tiles, a lone unit rejoins the pack legs. */
  val aloneExitTiles: Double = 13

  def shouldMarchAlone(nearestWarriorTiles: Double): Boolean =
    nearestWarriorTiles > aloneRadiusTiles

  private val alone = new Latch

  def reset(): Unit = { alone.reset() }

  def marchAlone(unitId: Int, nearestWarriorTiles: Double): Boolean =
    alone(
      unitId,
      fires    = nearestWarriorTiles > aloneEnterTiles,
      releases = nearestWarriorTiles < aloneExitTiles,
      initial  = shouldMarchAlone(nearestWarriorTiles))
}
