package Micro.Actions.Combat.Spells.Terran

import Micro.Actions.UnitControl
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

object Stim extends Action {
  override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
  override def isContainer: Boolean = true

  val stimHPThreshold = 15
  override def allowed(unit: FriendlyUnitInfo): Boolean = (
    unit.hitPoints >= stimHPThreshold
    && unit.confidence11 < 0.5
    && unit.transport.isEmpty
    && unit.canStim
    && ! unit.stimmed // TODO: stimFrames < With.latency.framesRemaining
    && (unit.matchups.targetsInRange.nonEmpty || unit.matchups.threatsInRange.nonEmpty)
  )
  
  override protected def perform(unit: FriendlyUnitInfo): UnitControl = {
    Commander.useTech(unit, Terran.Stim)
    UnitControl.claimedIf(! unit.ready)
  }
}
