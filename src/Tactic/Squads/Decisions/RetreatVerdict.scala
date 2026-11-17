package Tactic.Squads.Decisions

/**
 * Unified retreat verdict (shadow only, no game access): one object owning
 * the question "is this unit retreating", computed once per unit per frame
 * and read by every consumer. Today the march arm and the technique march
 * arm re-derive the answer from overlapping but different inputs (the
 * technique arm adds canFlee and the storm hold; the march arm ignores
 * both), on different phases, from different vintages of shouldFight - so
 * a committed hull (canFlee=false) marches Retreating while its technique
 * walks or duels, and the audit counts a mixed frame neither reader can
 * see. This kernel is the union of both arms; while it is a shadow
 * nothing reads it, disagreements only probe out (see CombatTechniques
 * syncConsensus). Migration: switch readers one by one behind the shadow
 * data, delete the old predicates last.
 */
object RetreatVerdict {

  /**
   * Unified verdict (pure numbers for the suite): a warrior that may flee,
   * holds no storm cast, lost the skirmish verdict and may retreat, holds
   * the retreat. Matches the march arm where the arms agree; where they
   * differ (committed hulls, casting Templars) it sides with the technique
   * arm and the disagreement probes out against the march label.
   */
  def shouldHold(
    isWarrior: Boolean,
    isReaver: Boolean,
    canFlee: Boolean,
    stormHold: Boolean,
    shouldFight: Boolean,
    totalHp: Int,
    unitsScore: Double,
    nearestArmedTiles: Double,
    proxyRush: Boolean): Boolean =
    isWarrior &&
      ! isReaver &&
      canFlee &&
      ! stormHold &&
      ! shouldFight &&
      RetreatAllowed.forUnit(totalHp, unitsScore, nearestArmedTiles, proxyRush)
}
