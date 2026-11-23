package Micro.Actions.Combat

import Micro.Actions.UnitControl
import Lifecycle.With
import Micro.Actions.Action
import Micro.Actions.Basic.WorkerCombatSupport
import Micro.Agency.Commander
import Micro.Actions.Combat.Maneuvering.Retreat
import Micro.Actions.Combat.Maneuvering.WorkerDarkTemplarFlee
import Micro.Actions.Combat.Maneuvering.StaticDefenseRepel
import Micro.Actions.Combat.Maneuvering.Stealth
import Micro.Actions.Combat.Decisioning.Actions.{Cast, Follow, Root}
import Micro.Actions.Combat.Spells.Terran.Stim
import Micro.Actions.Combat.Tactics._
import Micro.Actions.Combat.Tactics.Bunker._
import Micro.Actions.Combat.Tactics.Potshots._
import Micro.Actions.Protoss.Air.BeFlier
import Micro.Actions.Protoss.Arbiter.BeArbiter
import Micro.Actions.Protoss.DarkTemplar.BeDarkTemplar
import Micro.Actions.Protoss.Dragoon.BeDragoonVsSlowlot
import Micro.Actions.Protoss.Zealot.{BeRushingZealot, BeZealotVsZealot}
import Micro.Actions.Protoss.Carrier.BeCarrier
import ProxyBwapi.Races.Protoss
import Micro.Actions.Protoss.ReaverShuttle.BeReaver
import Micro.Actions.Terran.{BeCombatSCV, BeVulture}
import Micro.Actions.Zerg.BeMutalisk
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object Fight extends Action {
  override def isContainer: Boolean = true

  // Detectors cannot shoot: BeDetector drives them instead of a combat chain.
  override def allowed(unit: FriendlyUnitInfo): Boolean = (unit.canAttack || unit.canMove) && ! unit.agent.isScout && ! unit.unitClass.isDetector

  override def perform(unit: FriendlyUnitInfo): UnitControl = {
    // Opening stand (UMS mirrors): the line holds from frame zero
    // instead of trickling at an unseen enemy. Dragoons and zealots
    // only - other doctrines keep their own openers. Released on
    // contact, so real fights still belong to the duel below.
    if ((unit.is(Protoss.Dragoon) || unit.is(Protoss.Zealot)) && Tactic.Squads.MarchOrders.openingHold(unit)) {
      unit.agent.act("DressHold")
      Commander.hold(unit)

      return UnitControl.Claimed
    }
    // Templar on the line first: a visible blade near a worker owns the
    // frame through the dedicated move-away manager, never through
    // Retreat - the shortest line out, supported or not.
    if (unit.unitClass.isWorker && WorkerDarkTemplarFlee.run(unit)) return UnitControl.Claimed

    // Miners are not a combat squad. Without a fighting warrior nearby to
    // anchor them, they withdraw instead of trading alone after the army
    // has disengaged (worker rushes opt out explicitly).
    if (unit.unitClass.isWorker
      && With.blackboard.workerRushers() == 0
      && ! WorkerCombatSupport.supported(unit)) {
      Retreat(unit)

      return UnitControl.Claimed
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
    StaticDefenseRepel(unit)
    unit.agent.combat.perform(unit)
    OccupyBunker(unit)
    UnitControl.claimedIf(! unit.ready)
  }
}
