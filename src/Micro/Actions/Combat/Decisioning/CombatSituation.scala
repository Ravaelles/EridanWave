package Micro.Actions.Combat.Decisioning

import ProxyBwapi.UnitInfo.UnitInfo

/**
 * Read-only combat facts (the seam read-only behaviors depend on): the
 * target the ladder picked and the spacing around it. Acting behaviors
 * keep Combat (facts plus maneuvers); read-only ones take this, so the
 * type shows what they need and a future fake situation can drive them
 * without a game.
 */
trait CombatSituation {

  private[Decisioning] def target: Option[UnitInfo]

  private[Decisioning] def distances: CombatDistances
}
