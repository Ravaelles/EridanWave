package Micro.Actions.Protoss.Reaver

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

  /** Shields above which a Reaver tanks workers instead of boarding. */
  val standShields: Int = 70

  /**
   * Covered mineral-line stand: with only workers nearby and shields above
   * standShields the Reaver stays grounded and shoots instead of hopping
   * to the ride - workers cannot crack the shields before dying.
   */
  def shouldStandVsWorkers(onlyWorkersNear: Boolean, shields: Int): Boolean =
    onlyWorkersNear && shields > standShields
}
