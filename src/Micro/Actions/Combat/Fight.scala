package Micro.Actions.Combat

import Lifecycle.With
import Micro.Actions.Action
import Micro.Actions.Basic.WorkerCombatSupport
import Micro.Actions.Combat.Maneuvering.Retreat
import Micro.Actions.Combat.Decisioning.Actions.{Cast, Follow, Root}
import Micro.Actions.Combat.Maneuvering.Stealth
import Micro.Actions.Combat.Spells.Terran.Stim
import Micro.Actions.Combat.Tactics._
import Micro.Actions.Combat.Tactics.Bunker._
import Micro.Actions.Combat.Tactics.Potshots._
import Micro.Actions.Protoss._
import Micro.Actions.Protoss.Carrier.BeCarrier
import Micro.Actions.Protoss.Reaver.BeReaver
import Micro.Actions.Terran.{BeCombatSCV, BeVulture}
import Micro.Actions.Zerg.BeMutalisk
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object Fight extends Action {

  // Detectors cannot shoot: BeDetector drives them instead of a combat chain.
  override def allowed(unit: FriendlyUnitInfo): Boolean = (unit.canAttack || unit.canMove) && ! unit.agent.isScout && ! unit.unitClass.isDetector

  override def perform(unit: FriendlyUnitInfo): Unit = {
    // Miners are not a combat squad. Without a fighting warrior nearby to
    // anchor them, they withdraw instead of trading alone after the army
    // has disengaged (worker rushes opt out explicitly).
    if (unit.unitClass.isWorker
      && With.blackboard.workerRushers() == 0
      && ! WorkerCombatSupport.supported(unit)) {
      Retreat(unit)

      return
    }

    Follow(unit)
    StrategicNuke(unit)
    Cast(unit)
    Spot(unit)
    Stim(unit)
    EmergencyBunk(unit)
    Root(unit)
    BeFlier(unit)
    BeCombatSCV(unit)
    BeVulture(unit)
    BeRushingZealot(unit)
    BeZealotVsZealot(unit)
    BeDragoonVsSlowlot(unit)
    BeDarkTemplar(unit)
    BeCarrier(unit)
    BeArbiter(unit)
    BeReaver(unit)
    BeMutalisk(unit)
    Recharge(unit)
    Bust(unit)
    Stealth(unit)
    unit.agent.combat.perform(unit)
    OccupyBunker(unit)
  }
}
