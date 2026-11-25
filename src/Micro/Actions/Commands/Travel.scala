package Micro.Actions.Commands

import Micro.Actions.UnitControl
import Micro.Actions.Action
import Micro.Agency.Commander
import Micro.Coordination.Pathing.MicroPathing
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object Travel extends Action {
  override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
  override def isContainer: Boolean = true
  override def allowed(unit: FriendlyUnitInfo): Boolean = unit.canMove
  override def perform(unit: FriendlyUnitInfo): UnitControl = {
    if (unit.squadIntent.canSneak) {
      MicroPathing.tryMovingAlongTilePath(unit, MicroPathing.getSneakyPath(unit))

      return UnitControl.claimedIf(! unit.ready)
    }

    // Local march window (phase 2A): the line step bids March and
    // resolves at once - single-bid frames issue exactly like before.
    unit.agent.capturingBids = true
    unit.agent.bidReason = Some(Micro.Agency.UnitIntentionReason.March)
    Commander.move(unit)
    Micro.Agency.CommanderCore.resolveBids(unit)

    UnitControl.claimedIf(! unit.ready)
  }
}
