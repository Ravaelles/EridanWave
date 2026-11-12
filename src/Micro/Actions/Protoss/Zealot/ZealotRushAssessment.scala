package Micro.Actions.Protoss.Zealot

import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Whether a rushing Zealot should stand and trade at all. Separated from
 * target selection: "do I have business here" is a different question from
 * "what do I hit", and this one answers from range facts only.
 */
object ZealotRushAssessment {

  def shouldFight(zealot: FriendlyUnitInfo, fieldScore: Double): Boolean =
    fieldScore > 0 || canPotshot(zealot) || hugging(zealot)

  /** Nobody is worth scoring in place, but a shooter can be answered right now. */
  def canPotshot(zealot: FriendlyUnitInfo): Boolean =
    zealot.readyForAttackOrder && zealot.matchups.targetsInRange.exists(_.canAttack(zealot))

  /** Something in range outranges us; running costs more than one swing. */
  def hugging(zealot: FriendlyUnitInfo): Boolean =
    zealot.matchups.targetsInRange.exists(t => t.canAttack(zealot) && t.pixelRangeAgainst(zealot) > 32)
}
