package Micro.Actions.Protoss.Zealot

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.Time.Minutes

import scala.language.postfixOps

object BeZealotVsZealot extends Action {

  override def allowed(unit: FriendlyUnitInfo): Boolean = (
    With.frame < Minutes(5)()
      && Protoss.Zealot(unit)
      && With.enemies.exists(_.isProtoss)
      && unit.matchups.threatsInPixels(128).forall(_.isAny(Protoss.Zealot, Protoss.Probe)))

  override def perform(unit: FriendlyUnitInfo): Unit = {
    val obviousTarget = Maff.minBy(unit.matchups.targetsInPixels(16))(t => t.totalHealth / (1.0 + t.matchups.threatsInRange.count(unit!=)))
    unit.agent.toAttack = obviousTarget
    val target = obviousTarget.orElse(unit.matchups.targetNearest)

    DuelDecision.decide(DuelDecision.DuelFacts(
      outnumbered = unit.matchups.threatsInPixels(24).filter(Protoss.Zealot).count(_.presumptiveTarget.contains(unit)) > 1,
      dying       = unit.totalHealth < 14 && unit.matchups.threatsInPixels(64).exists(t => t.totalHealth > 14 || t.cooldownLeft < unit.cooldownLeft),
      duelling    = target.exists(t => unit.matchups.threatsInPixels(unit.pixelsToGetInRange(t) + 32).forall(t==) && unit.totalHealth * (1 + unit.confidence11) > t.totalHealth),
      outnumbering = ! unit.matchups.threatsInPixels(64).exists(_.presumptiveTarget.contains(unit))
        && target.exists(enemy =>
        enemy.presumptiveTarget.exists(ally =>
          ally.friendly.exists(_.agent.shouldFight)
          || unit.totalHealth * (1 + unit.confidence11) > enemy.totalHealth)))) match {
      case DuelDecision.Outnumbered =>
        unit.agent.fightReason = "Outnumbered"
        unit.agent.shouldFight = false

        return

      case DuelDecision.Dying =>
        unit.agent.fightReason = "Dying"
        unit.agent.shouldFight = false

        return

      case DuelDecision.Duelling =>
        unit.agent.fightReason = "Duelling"
        unit.agent.shouldFight = true
        unit.agent.toAttack    = target

        return

      case DuelDecision.Outnumbering =>
        unit.agent.fightReason = "Outnumbering"
        unit.agent.shouldFight = true
        unit.agent.toAttack    = target
      case DuelDecision.Undecided =>
    }
  }
}


