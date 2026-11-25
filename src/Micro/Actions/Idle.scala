package Micro.Actions

import Micro.Actions.Basic._
import Micro.Actions.Combat.Decisioning.Skirmish.FightOrFlee
import Micro.Actions.Combat.Fight
import Micro.Actions.Combat.Tactics.Bunker.Unbunk
import Micro.Actions.Commands.{Attack, Travel}
import Micro.Actions.Protoss.Detector.BeDetector
import Micro.Actions.Protoss.DarkTemplar.DarkTemplarRaid
import Micro.Actions.Protoss.Support.{Meld, ReloadInterceptors, ReloadScarabs}
import Micro.Actions.Scouting.Scout
import Micro.Actions.Terran.{FinishConstruction, GetRepairedMobile, Land, Liftoff, Repair, Scan}
import Micro.Actions.Transportation.Transport
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Production.core.Produce

object Idle extends Action {
  override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
  override def isContainer: Boolean = true

  @inline
  final override def allowed(unit: FriendlyUnitInfo): Boolean = true

  @inline
  final override def perform(unit: FriendlyUnitInfo): UnitControl = {
    var i = 0
    while (i < actions.length) {
      actions(i)(unit)
      i += 1
    }
    UnitControl.Claimed
  }

  private val actions = Array[Action](
    Cancel,
    Liftoff,
    Land,
    Unbunk,
    Meld,
    Build,
    FinishConstruction,
    FightOrFlee,
    Repair,
    GetRepairedMobile,
    Gather,
    Addon,
    Scan,
    Unstick,
    Produce,
    Rally,
    ReloadInterceptors,
    ReloadScarabs,
    Transport,
    Scout,
    Bunk,
    Crack,
    BeDetector,
    DarkTemplarRaid,
    Fight,
    Attack,
    Travel
  )
}
