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
    ! standVsWorkers(situation) &&
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
   * Covered mineral-line stand: with only workers nearby and shields above
   * standShields the Reaver stays grounded and shoots instead of hopping
   * to the ride - boarding first would circle without dealing damage.
   */
  private def standVsWorkers(situation: ReaverSituation): Boolean =
    ReaverBoardLogic.shouldStandVsWorkers(situation.onlyWorkersNear, situation.shields)

  private def mustShootFirst(situation: ReaverSituation): Boolean = {
    val ready = ReaverFireDiscipline.shotReady(situation.scarabs, situation.hasActiveScarab, situation.cooling)
    ReaverFireDiscipline.mustShootFirst(ready, situation.pressured || situation.holdTarget)
  }

  def act(unit: FriendlyUnitInfo): Unit = {
    unit.agent.act("Hop")
    ReaverActions.clickShuttle(unit)
  }
}
