package Micro.Actions.Commands

import Micro.Actions.UnitControl
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object Attack extends Action {
  override def isContainer: Boolean = true
  override def allowed(unit: FriendlyUnitInfo): Boolean =
    unit.canAttack && unit.agent.toAttack.isDefined &&
      // Opening stand owns the frame (never the duel): the line forms
      // up instead of chasing a far contact through an unformed line.
      // Warriors only, UMS only, released on contact - the duel below
      // shoots hittable contacts through every other leg anyway.
      ! Tactic.Squads.MarchOrders.openingHold(unit)
  override def perform(unit: FriendlyUnitInfo): UnitControl = {
    Commander.attack(unit)
    UnitControl.claimedIf(! unit.ready)
  }
}
