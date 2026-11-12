package Macro.Scheduling.Simulation

import ProxyBwapi.Techs.Techs
import ProxyBwapi.UnitClasses.UnitClasses
import ProxyBwapi.Upgrades.Upgrades

/** Carries a [[MacroState]] forward by one event.
  *
  * The collections start out shared with the previous state and are cloned only
  * when this event writes them. Cloning everything unconditionally is correct
  * but makes every step of the simulation pay for five maps.
  */
object MacroStateAdvance {

  def advance(
      last            : MacroState,
      next            : MacroState,
      event           : MacroEvent,
      dFrames         : Int,
      mineralsPerFrame: Double,
      gasPerFrame     : Double)
      : Unit = {
    next.minerals          = last.minerals        + event.dMinerals + (dFrames * mineralsPerFrame).toInt
    next.gas               = last.gas             + event.dGas      + (dFrames * gasPerFrame).toInt
    next.supplyAvailable   = Math.min(400, last.supplyAvailable + event.dSupplyAvailable)
    next.supplyUsed        = last.supplyUsed      + event.dSupplyUsed
    next.mineralPatches    = last.mineralPatches  + event.dMineralPatches
    next.geysers           = last.geysers         + event.dGeysers
    next.techs             = last.techs
    next.upgrades          = last.upgrades
    next.unitsExtant       = last.unitsExtant
    next.unitsComplete     = last.unitsComplete
    next.unitsCompleteASAP = last.unitsCompleteASAP
    next.producers         = last.producers
    if (event.dUpgrade          != Upgrades.None)                                             next.upgrades          = next.upgrades.clone
    if (event.dUnitExtant1      != UnitClasses.None || event.dUnitExtant2 != UnitClasses.None) next.unitsExtant       = next.unitsExtant.clone
    if (event.dUnitComplete     != UnitClasses.None)                                          next.unitsComplete     = next.unitsComplete.clone
    if (event.dUnitCompleteASAP != UnitClasses.None)                                          next.unitsCompleteASAP = next.unitsCompleteASAP.clone
    if (event.dProducer1        != UnitClasses.None || event.dProducer2 != UnitClasses.None || event.dAddon != UnitClasses.None) next.producers = next.producers.clone
    if (event.dTech             != Techs.None)        next.techs                                       += event.dTech
    if (event.dUpgrade          != Upgrades.None)     next.upgrades(event.dUpgrade)                    =  event.dUpgradeLevel
    if (event.dUnitExtant1      != UnitClasses.None)  next.unitsExtant(event.dUnitExtant1)             += event.dUnitExtant1N
    if (event.dUnitExtant2      != UnitClasses.None)  next.unitsExtant(event.dUnitExtant2)             += event.dUnitExtant2N
    if (event.dUnitComplete     != UnitClasses.None)  next.unitsComplete(event.dUnitComplete)          += event.dUnitCompleteN
    if (event.dUnitCompleteASAP != UnitClasses.None)  next.unitsCompleteASAP(event.dUnitCompleteASAP)  += event.dUnitCompleteASAPN
    if (event.dProducer1        != UnitClasses.None)  next.producers(event.dProducer1)                 += event.dProducer1N
    if (event.dProducer2        != UnitClasses.None)  next.producers(event.dProducer2)                 += event.dProducer2N
    if (event.dAddon            != UnitClasses.None)  next.producers(event.dAddon)                     += event.dAddonN
  }
}
