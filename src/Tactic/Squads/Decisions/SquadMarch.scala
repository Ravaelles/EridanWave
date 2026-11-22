package Tactic.Squads.Decisions

/**
 * When to build a march formation (Stardust formArc-before-contact): not only
 * once the squad voted to fight, but as soon as enemies are known en route,
 * so the line assembles before contact instead of mid-fight.
 *
 * Pure predicate so it stays unit-testable without a game.
 */
object SquadMarch {

  def shouldFormMarch(fightConsensus: Boolean, hasEnemiesEnRoute: Boolean): Boolean = {
    fightConsensus || hasEnemiesEnRoute
  }

  /**
   * UMS opening formation: stand the line up from the first frame, even
   * with nothing known yet. Owner order from the ZealDrag mirror (the
   * line never formed before contact, so it attacked in a blob). No
   * frame bound: the march formation is the normal state once enemies
   * are known, so forming it early only extends the formed period
   * instead of flapping at first contact. Ladder keeps the old gate.
   */
  def shouldFormOpening(umsMode: Boolean, hasEnemiesEnRoute: Boolean, fightConsensus: Boolean): Boolean =
    umsMode && ! hasEnemiesEnRoute && ! fightConsensus
}
