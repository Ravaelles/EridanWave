package Micro.Actions.Protoss

/**
 * Pure boarding decision for a cooling Reaver (no game access).
 *
 * The caller reads the world (cooldown, outranging threats, shot
 * availability, pressure); this only combines them. A cooling Reaver with
 * nothing to answer boards only under pressure - boarding on an empty
 * battlefield just rooms it in the Shuttle instead of standing ready.
 */
object ReaverBoardLogic {

  /**
   * @param cooling   cooldown longer than a drop-reset would give
   * @param outranged a threat in range outranges us (Siege Tanks)
   * @param shotless  no Scarab ready and no target to spend it on
   * @param pressured a threat in range or close enough to hit us
   * @return true when the Reaver should board instead of waiting out in the open
   */
  def shouldBoardCooling(cooling: Boolean, outranged: Boolean, shotless: Boolean, pressured: Boolean): Boolean =
    cooling && (outranged || (shotless && pressured))
}
