package Lifecycle

/**
 * Atlantis-style own-race helpers.
 */
object We {

  def terran  : Boolean = With.self.isTerran

  def protoss : Boolean = With.self.isProtoss

  def zerg    : Boolean = With.self.isZerg
}
