package Micro.Actions.Protoss.Reaver

/**
 * Pure fire discipline (no game access): a Reaver that just dropped with
 * a Scarab ready and an enemy nearby must spend the shot before it is
 * allowed back aboard. Boarding first is the infinite circle - unload,
 * instant reboard, fly around taking fire, repeat - that never deals
 * damage. A cooling Reaver (shot spent or none ready) is exempt: it has
 * nothing to hold for.
 */
object ReaverFireDiscipline {

  /**
   * @param shotReady  Scarab loaded, none in flight, cooldown near ready
   * @param enemyNear  a threat in range, close enough to hit, or held to fire on
   * @return true when boarding is banned until the shot is spent
   */
  def mustShootFirst(shotReady: Boolean, enemyNear: Boolean): Boolean =
    shotReady && enemyNear
}
