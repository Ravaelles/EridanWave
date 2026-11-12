package Tactic.Squads

import Mathematics.Maff
import Mathematics.Points.Pixel
import Micro.Formation.{FormationStyleDisengage, FormationStyleGuard}
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.Decisions.{MarchDress, SquadRally, SuicideChargeSeam}
import Utilities.?

/**
 * March orders (extracted from SquadAutomation): where each squad unit
 * walks. One reason to change: the march rules (rally stragglers, close
 * ranks, dress leaders, send Reavers home, never hold a suicide charge).
 * Targeting stays in SquadAutomation, formations in Micro.Formation.
 */
object MarchOrders {

  def sendUnits(squad: Squad, units: Iterable[FriendlyUnitInfo], defaultReturn: Option[Pixel] = None): Unit = {
    // Common march reference: the pack mean distance to the squad goal, in tiles.
    // Leaders ahead of it wait (MarchDress); laggards march or rally back.
    val meanDistToGoalTiles = Maff.mean(units.view.map(_.pixelDistanceCenter(squad.vicinity))) / 32.0
    units.foreach(unit => sendUnit(squad, unit, meanDistToGoalTiles, defaultReturn))
  }

  private def sendUnit(squad: Squad, unit: FriendlyUnitInfo, meanDistToGoalTiles: Double, defaultReturn: Option[Pixel]): Unit = {
    val finalReturn = getReturn(unit, squad, defaultReturn)
    val (terminus, marchLabel) = marchTerminus(squad, unit, meanDistToGoalTiles, defaultReturn)
    Debugging.CherryVis.CherryVis.decision(unit.id,
      ?(squad.fightConsensus, marchLabel, "Returning to slot"))
    unit.intend(squad)
      .setTerminus(?(squad.fightConsensus, terminus, finalReturn))
      .setRedoubt(finalReturn)
  }

  // The march rules, innermost first: rallying stragglers beat slots,
  // closing ranks beats marching alone, dressing beats everything but
  // a suicide charge (which skips the dressing room entirely).
  private def marchTerminus(squad: Squad, unit: FriendlyUnitInfo, meanDistToGoalTiles: Double, defaultReturn: Option[Pixel]): (Pixel, String) = {
    val finalTravel = getTravel(unit, squad, defaultReturn)
    // Suicide charge: this close to the Zerg main there is no holding,
    // rallying, regrouping or dressing - only the attack. Committed like
    // a drop so no verdict can call it off.
    val suicide = SuicideChargeSeam.inZone(unit)
    if (suicide) unit.agent.commit = true
    // Straggler rally (Stardust): a unit lagging behind the marching pack
    // rejoins the pack centroid first instead of walking alone to its slot.
    // Returning units keep their own slots - they are meant to disperse.
    val pack       = ?(unit.flying, squad.centroidAir, squad.centroidGround)
    val rallying   = ! suicide && squad.fightConsensus && SquadRally.isRallying(unit.pixel, pack)
    val finalMarch = ?(rallying, pack, finalTravel)
    // Close ranks on the march: a unit adrift from its nearest ally
    // rejoins instead of milling alone. Consensus-only, so guards and
    // defenders keep their posts instead of bunching up.
    val nearestAlly  = Maff.minBy(squad.units.view.filter(_ != unit))(_.pixelDistanceCenter(unit.pixel))
    val joiningAlly  = ! suicide && squad.fightConsensus && nearestAlly.exists(a => SquadRally.shouldJoinAlly(unit.tileDistanceCenter(a.pixel)))
    val joinPixel    = nearestAlly.map(_.pixel)
    // March dressing: a leader running ahead of the pack holds position so
    // the line arrives together instead of trickling in. Waiting zeroes the
    // stuck counter (tryingToMove is false), so Unstick stays out of it.
    // Reavers never hold: a slow hull waiting ahead of a stalled pack
    // freezes next to the enemy, so it walks back to the pack instead.
    val waiting      = ! suicide && squad.fightConsensus && MarchDress.shouldWait(unit.tileDistanceCenter(squad.vicinity), meanDistToGoalTiles)
    val reaverToArmy = Protoss.Reaver(unit) && MarchDress.shouldWait(unit.tileDistanceCenter(squad.vicinity), meanDistToGoalTiles)
    val terminus     = ?(waiting && ! reaverToArmy, unit.pixel, ?(joiningAlly, joinPixel.getOrElse(finalMarch), ?(reaverToArmy, pack, finalMarch)))
    val label        = ?(waiting && ! reaverToArmy, "Waiting for pack", ?(rallying || reaverToArmy, "Rallying to pack centroid", "Marching to formation slot"))
    (terminus, label)
  }

  def getReturn(unit: FriendlyUnitInfo, squad: Squad, defaultReturn: Option[Pixel] = None): Pixel = squad
    .formations
    .find(f => f.style == FormationStyleGuard || f.style == FormationStyleDisengage)
    .filter(_.placements.contains(unit))
    .map(_.placements(unit))
    .orElse(defaultReturn)
    .getOrElse(squad.homeConsensus)

  def getTravel(unit: FriendlyUnitInfo, squad: Squad, defaultReturn: Option[Pixel] = None): Pixel =
    ?(squad.hasGround, None, Some(squad.vicinity))
      .orElse(
        squad
          .formations
          .headOption
          .find(_.placements.contains(unit))
          .map(_.placements(unit)))
      .getOrElse(?(squad.fightConsensus, squad.vicinity, getReturn(unit, squad, defaultReturn)))
}
