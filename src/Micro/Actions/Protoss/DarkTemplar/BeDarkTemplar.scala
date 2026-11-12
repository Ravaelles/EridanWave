package Micro.Actions.Protoss.DarkTemplar

import Lifecycle.With
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object BeDarkTemplar extends Action {

  override def allowed(unit: FriendlyUnitInfo): Boolean = Protoss.DarkTemplar(unit)

  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    if (unit.readyForAttackOrder && With.enemies.exists(_.isTerran)) {
      val mines = unit.matchups.threats.filter(Terran.SpiderMine)
      if (mines.nonEmpty) {
        val mine = mines.minBy(_.pixelDistanceEdge(unit))
        val sighting = SpiderMineSniping.MineSighting(
          targetedUs        = mine.orderTarget.contains(unit),
          distPixels        = mine.pixelDistanceEdge(unit),
          neighborsInRadius = mines.count(_.pixelDistanceEdge(mine) < SpiderMineSniping.radiusPixels),
          visible           = mine.visible,
          cloaked           = mine.effectivelyCloaked)
        if (SpiderMineSniping.shouldSnipe(sighting)) {
          unit.agent.toAttack = Some(mine)
          Commander.attack(unit)
        }
      }
    }
  }
}
