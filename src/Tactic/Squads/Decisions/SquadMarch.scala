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
}
