package Micro.Actions.Protoss.DarkTemplar

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.Time.Minutes

/**
 * Dark Templar brain: snipe lone mines, otherwise hunt visible hittable
 * enemies first and freshly remembered contacts second - never unseen
 * ground. March and formations are bypassed elsewhere (no slots, no
 * waiting, no rally), so the hunt target here is the whole march program;
 * with no contact the Templar holds its decision and travels with the
 * squad instead of scouting alone.
 */
object BeDarkTemplar extends Action {

  override def allowed(unit: FriendlyUnitInfo): Boolean = Protoss.DarkTemplar(unit)

  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    snipeSpiders(unit)
    if (unit.agent.toAttack.isEmpty) hunt(unit)
  }

  private def snipeSpiders(unit: FriendlyUnitInfo): Unit = {
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

  private def hunt(unit: FriendlyUnitInfo): Unit = {
    val target = visibleHittable(unit).orElse(rememberedContact(unit))
    unit.agent.toAttack = target
    target match {
      case Some(t) =>
        unit.agent.act("DarkTemplarHunt")
        if (unit.inRangeToAttack(t)) Commander.attack(unit)
        else {
          unit.agent.destinationNext.set(t.pixel)
          Commander.move(unit)
        }
      case None =>
        unit.agent.act("DarkTemplarHold")
    }
  }

  private def visibleHittable(unit: FriendlyUnitInfo): Option[UnitInfo] =
    Maff.minBy(unit.matchups.targets.filter(t => t.visible && unit.canAttack(t)))(_.pixelDistanceEdge(unit))

  private def rememberedContact(unit: FriendlyUnitInfo): Option[UnitInfo] =
    Maff.maxBy(With.units.enemy.filter(e =>
      ! e.visible
      && unit.canAttack(e)
      && With.framesSince(e.lastSeen) < Minutes(3)()))(_.lastSeen)
}
