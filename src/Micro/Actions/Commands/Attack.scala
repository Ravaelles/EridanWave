package Micro.Actions.Commands

import Micro.Actions.UnitControl
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object Attack extends Action {
  override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
  override def isContainer: Boolean = true
  override def allowed(unit: FriendlyUnitInfo): Boolean =
    unit.canAttack && unit.agent.toAttack.isDefined
  override def perform(unit: FriendlyUnitInfo): UnitControl = {
    // Local duel window (phase 2A): the deliberate attack bids Duel and
    // resolves at once - single-bid frames issue exactly like before.
    unit.agent.capturingBids = true
    unit.agent.bidReason = Some(Micro.Agency.UnitIntentionReason.Duel)
    Commander.attack(unit)
    Micro.Agency.CommanderCore.resolveBids(unit)

    UnitControl.claimedIf(! unit.ready)
  }
}
