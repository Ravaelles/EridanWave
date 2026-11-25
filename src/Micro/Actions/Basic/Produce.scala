package Micro.Actions.Basic

import Micro.Actions.UnitControl
import Lifecycle.With
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Production.core.Produce

object Produce extends Action {
  override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
  override def isContainer: Boolean = true
  
  override def allowed(unit: FriendlyUnitInfo): Boolean = (
    (unit.squadIntent.toTrain.isDefined
      || unit.squadIntent.toTech.isDefined
      || unit.squadIntent.toUpgrade.isDefined)
      && (unit.trainingQueue.isEmpty || unit.trainee.exists(_.remainingCompletionFrames <= 1 + With.latency.latencyFrames + With.reaction.agencyMax))
      && unit.trainingQueue.size < 2)
  
  override def perform(unit: FriendlyUnitInfo): UnitControl = {
    val hadOrder = unit.squadIntent.toTrain.isDefined || unit.squadIntent.toTech.isDefined || unit.squadIntent.toUpgrade.isDefined
    if (unit.squadIntent.toTrain.isDefined) {
      if (With.framesSince(unit.squadIntent.frameCreated) < Math.max(128, unit.squadIntent.toTrain.get.buildFrames / 2)) {
        Commander.build(unit, unit.squadIntent.toTrain.get)
      }
    } else if (unit.squadIntent.toTech.isDefined) {
      Commander.tech(unit, unit.squadIntent.toTech.get)
    } else if (unit.squadIntent.toUpgrade.isDefined) {
      Commander.upgrade(unit, unit.squadIntent.toUpgrade.get)
    }
    unit.squadIntent.toTrain = None // Avoid training repeatedly
    unit.squadIntent.toTech = None // Avoid teching repeatedly (mostly impacts failure to renew the desire to tech)
    unit.squadIntent.toUpgrade = None //Avoid upgrading repeatedly
    UnitControl.claimedIf(hadOrder)
  }
}
