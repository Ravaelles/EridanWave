package Micro.Actions.Zerg

import Micro.Actions.UnitControl
import Lifecycle.With
import Micro.Actions.Action
import Micro.Actions.Combat.Maneuvering.Retreat
import Micro.Coordination.Pathing.MicroPathing
import Micro.Targeting.Target
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitTracking.AliveEnemies
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object BeMutalisk extends Action {
  override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
  override def appliesToUnit(unit: FriendlyUnitInfo): Boolean = Zerg.Mutalisk(unit)

  override def allowed(unit: FriendlyUnitInfo): Boolean = Zerg.Mutalisk(unit)

  object MutaliskRecovering
  object MutaliskVolley

  // A recovering Mutalisk with a safety margin fights back instead of fleeing.
  private def recoverByFighting(unit: FriendlyUnitInfo): Unit = {
    unit.agent.toAttack = Target.best(unit, AliveEnemies.get().filter(_.tile.enemyRangeAir <= 0))
    val target = unit.agent.toAttack
    if (target.isEmpty) return

    if (unit.framesToGetInRange(target.get) < With.reaction.agencyMax) {
      unit.agent.decideFight(true)
    } else {
      unit.agent.terminus.set(target.get.pixel)
      val path = MicroPathing.getThreatAwarePath(unit, preferHome = true)
      if (path.pathExists) {
        MicroPathing.tryMovingAlongTilePath(unit, path)
      }
    }
  }

  override protected def perform(unit: FriendlyUnitInfo): UnitControl = {
    if (unit.cooldownLeft < 24) {
      unit.agent.remove(MutaliskVolley)
    }

    if (unit.hitPoints > 100) {
      unit.agent.remove(MutaliskRecovering)
    } else if (unit.hitPoints < 30 && ! unit.matchups.targetNearest.exists(_.matchups.targetNearestInRange.exists(_ !=  unit))) {
      unit.agent.put(MutaliskRecovering)
    }

    val recovering = unit.agent.contains(MutaliskRecovering)
    if (recovering) {
      unit.agent.decideFight(false, "Recover")
      unit.agent.origin.set(With.geography.bases.minBy(_.lastFrameScoutedByUs).townHallArea.center)
      if (unit.matchups.framesOfSafety <= 0) {
        Retreat.perform(unit)
      } else {
        recoverByFighting(unit)
      }
    } else if (unit.confidence01 > 0.35 && ! unit.matchups.threatDeepest.exists(_.isAny(Terran.Goliath, Terran.MissileTurret, Protoss.Dragoon, Protoss.PhotonCannon, Zerg.Mutalisk, Zerg.SporeColony))) {
      val mutalisks = unit.tileArea.expand(5).tiles.flatMap(_.units.flatMap(_.friendly).filter(Zerg.Mutalisk))
      if (mutalisks.forall(u => u.readyForAttackOrder || u.agent.contains(MutaliskVolley))) {
        unit.agent.decideFight(true, "MutaVolley")
        unit.agent.put(MutaliskVolley)
      }
    }
    UnitControl.claimedIf(! unit.ready)
  }
}
