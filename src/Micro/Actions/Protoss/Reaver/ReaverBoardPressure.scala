package Micro.Actions.Protoss.Reaver

import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Board under pressure - but never interrupt a healthy committed attack
 * (Atlantis dontInterruptAttack). Covers doom, Scarab targeting, evac
 * after firing hurt, failing shields, fresh hits, long hikes and the
 * cooling-with-nothing-to-answer rule.
 */
object ReaverBoardPressure {

  def applies(situation: ReaverSituation): Boolean =
    ! situation.loaded && ! situation.committed && ! mustShootFirst(situation) &&
      ! (situation.containing && ! situation.pressured) && (
      situation.doomed ||
        situation.targetedByScarab ||
        (situation.cooldownLeft >= 10 && situation.shieldsDown >= 45
          && situation.framesSinceFired < 30 && situation.pressuredClose) ||
        (situation.shieldsDown >= 20 && (situation.shieldsDown >= 38 || situation.scarabs == 0)
          && situation.pressuredClose) ||
        (situation.framesSinceHit < 50 && situation.shieldsDown >= 18) ||
        situation.hikingFar ||
        ReaverBoardLogic.shouldBoardCooling(situation.cooling, situation.outranged, situation.shotless, situation.pressured))

  /**
   * A dropped Reaver with a Scarab ready and an enemy nearby holds to
   * spend the shot; boarding first would circle without dealing damage.
   */
  private def mustShootFirst(situation: ReaverSituation): Boolean = {
    val shotReady = situation.scarabs > 0 && ! situation.hasActiveScarab && ! situation.cooling
    ReaverFireDiscipline.mustShootFirst(shotReady, situation.pressured || situation.holdTarget)
  }

  def act(unit: FriendlyUnitInfo): Unit = {
    unit.agent.act("Hop")
    BeReaver.clickShuttle(unit)
  }
}
