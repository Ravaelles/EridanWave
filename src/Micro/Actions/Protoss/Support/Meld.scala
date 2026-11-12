package Micro.Actions.Protoss.Support

import Micro.Actions.Action
import Micro.Actions.Combat.Maneuvering.Retreat
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.Time.Seconds

object Meld extends Action {
  
  override def allowed(unit: FriendlyUnitInfo): Boolean = (
    unit.intent.shouldMeld
    && unit.isAny(Protoss.HighTemplar, Protoss.DarkTemplar))
  
  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    // No return after the retreat: the meld below overrides it whenever a
    // partner is present, so an unsafe pair still Archons on the spot while
    // a lone Templar keeps the retreat move. Whether melding under fire
    // beats running is game evidence, not this file, to decide.
    if (unit.matchups.framesOfSafety < Seconds(15)() && ! unit.base.exists(_.isOurs)) {
      Retreat(unit)
    }

    val besties = unit
      .metro.map(_.units)
      .getOrElse(unit.zone.units)
      .filter(other =>
        other != unit
        && other.friendly.exists(_.intent.shouldMeld)
        && other.unitClass == unit.unitClass)
    
    if (besties.nonEmpty) {
      val bestBestie = besties.minBy(_.pixelDistanceTravelling(unit.pixel))
      Commander.useTechOnUnit(
        unit,
        if (Protoss.HighTemplar(unit)) Protoss.ArchonMeld else Protoss.DarkArchonMeld,
        bestBestie)
    }
  }
}
