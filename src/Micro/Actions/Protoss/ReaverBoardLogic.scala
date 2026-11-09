package Micro.Actions.Protoss

/**
 * Pure boarding decision for a cooling Reaver (no game access).
 *
 * The caller reads the world (cooldown, outranging threats, shot
 * availability); this only combines them. That split keeps the
 * don't-sit-under-Siege-Tanks rule unit-testable.
 */
object ReaverBoardLogic {

  /**
   * @param cooling   cooldown longer than a drop-reset would give
   * @param outranged a threat in range outranges us (Siege Tanks)
   * @param shotless  no Scarab ready and no target to spend it on
   * @return true when the Reaver should board instead of waiting out in the open
   */
  def shouldBoardCooling(cooling: Boolean, outranged: Boolean, shotless: Boolean): Boolean =
    cooling && (outranged || shotless)
}
