package Lifecycle

import bwapi.Race

/**
 * Atlantis-style enemy race helpers. Any-enemy semantics, so they stay
 * correct in FFA games too. Random means the lobby pick, not an unscouted
 * race (that is still `Unknown` current).
 */
object Enemy {

  def isTerran  : Boolean = With.enemies.exists(_.isTerran)

  def isProtoss : Boolean = With.enemies.exists(_.isProtoss)

  def isZerg    : Boolean = With.enemies.exists(_.isZerg)

  def isRandom  : Boolean = With.enemies.exists(_.raceInitial == Race.Unknown)
}
