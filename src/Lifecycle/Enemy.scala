package Lifecycle

import bwapi.Race

/**
 * Atlantis-style enemy race reads, one terse call per race:
 * `Enemy.terran()` instead of the five-word exists-chain. Any-enemy
 * semantics, so they stay correct in FFA games too. Random means the
 * lobby pick, not an unscouted race (that is still `Unknown` current).
 */
object Enemy {

  def terran(): Boolean = With.enemies.exists(_.isTerran)

  def protoss(): Boolean = With.enemies.exists(_.isProtoss)

  def zerg(): Boolean = With.enemies.exists(_.isZerg)

  def random(): Boolean = With.enemies.exists(_.raceInitial == Race.Unknown)
}
