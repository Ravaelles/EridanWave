package Micro.Actions.Protoss

import Micro.Actions.Action
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * A shield-stripped Zealot among ranged allies does not duel the melee
 * pack - it decoys it, fleeing while the Dragoons shoot unattacked.
 * Same shouldFight=false withdrawal as the Outnumbered/Dying rules next
 * door; the retreat itself runs downstream.
 */
object BeDecoyZealot extends Action {

  override def allowed(unit: FriendlyUnitInfo): Boolean = Protoss.Zealot(unit)

  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    val radiusPixels = ZealotDecoy.decoyDistTiles * 32
    val rangedAllies = unit.matchups.allies.count(a =>
      a.unitClass.canAttack && ! a.unitClass.melee && a.pixelDistanceEdge(unit) < radiusPixels)
    if (rangedAllies < 1) return

    val meleeFoes = unit.matchups.threats.count(t =>
      t.unitClass.melee && t.pixelDistanceEdge(unit) < radiusPixels)
    if (ZealotDecoy.shouldDecoy(unit.shieldPoints, rangedAllies, meleeFoes)) {
      unit.agent.fightReason = "Decoy"
      unit.agent.shouldFight = false
    }
  }
}
