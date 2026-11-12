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
    val assault = SuicideChargeSeam.inMainAssault(unit)
    val (terminus, marchLabel) = marchTerminus(squad, unit, meanDistToGoalTiles, defaultReturn)
    Debugging.CherryVis.CherryVis.decision(unit.id,
      ?(squad.fightConsensus || assault, marchLabel, "Returning to slot"))
    unit.intend(squad)
      .setTerminus(?(squad.fightConsensus || assault, terminus, finalReturn))
      .setRedoubt(finalReturn)
  }

  // The march rules, innermost first: rallying stragglers beat slots,
  // closing ranks beats marching alone, dressing beats everything but
  // a suicide charge (which skips the dressing room entirely).
  // Dark Templars run their own hunt (visible or remembered contacts only):
  // they never dress, rally, or take formation slots, so they march
  // straight at the squad goal instead of posing with the line.
  private def marchTerminus(squad: Squad, unit: FriendlyUnitInfo, meanDistToGoalTiles: Double, defaultReturn: Option[Pixel]): (Pixel, String) = {
    val finalTravel = getTravel(unit, squad, defaultReturn)
    val suicide = SuicideChargeSeam.inZone(unit)
    if (suicide) unit.agent.commit = true
    // Dark Templars run their own hunt (visible or remembered contacts only):
    // they never dress, rally, or take formation slots, so they march
    // straight at the squad goal instead of posing with the line.
    if (Protoss.DarkTemplar(unit)) return (squad.vicinity, "DarkTemplarHunt")

    // Main assault: a ground warrior inside the enemy main never poses,
    // rallies or walks home - it commits (no retreat, attack only) and
    // marches straight at the squad goal past every formation leg below.
    if (SuicideChargeSeam.inMainAssault(unit)) {
      unit.agent.commit = true

      return (squad.vicinity, "MainAssault")
    }

    val (finalMarch, rallying) = rallyLeg(squad, unit, finalTravel, suicide)
    val joinPixel              = joinLeg(squad, unit, suicide)
    val (holding, homing)      = dressLeg(squad, unit, meanDistToGoalTiles, suicide)
    val pack                   = ?(unit.flying, squad.centroidAir, squad.centroidGround)
    val terminus = ?(holding, unit.pixel, joinPixel.getOrElse(?(homing, pack, finalMarch)))
    val label    = ?(holding, "Waiting for pack", ?(rallying || homing, "Rallying to pack centroid", "Marching to formation slot"))
    (terminus, label)
  }

  // Straggler rally (Stardust): a unit lagging behind the marching pack
  // rejoins the pack centroid first instead of walking alone to its slot.
  // Returning units keep their own slots - they are meant to disperse.
  // Slow hulls (Reavers) rally on a longer leash so they keep their slot
  // and their shot instead of chasing the pack all march.
  private def rallyLeg(squad: Squad, unit: FriendlyUnitInfo, finalTravel: Pixel, suicide: Boolean): (Pixel, Boolean) = {
    val pack     = ?(unit.flying, squad.centroidAir, squad.centroidGround)
    val leash    = SquadRally.rallyRadiusFor(Protoss.Reaver(unit))
    val rallying = ! suicide && squad.fightConsensus && SquadRally.isRallying(unit.pixel, pack, leash)
    (?(rallying, pack, finalTravel), rallying)
  }

  // Close ranks on the march: a unit adrift from its nearest ally
  // rejoins instead of milling alone. Consensus-only, so guards and
  // defenders keep their posts instead of bunching up.
  private def joinLeg(squad: Squad, unit: FriendlyUnitInfo, suicide: Boolean): Option[Pixel] = {
    val nearestAlly  = Maff.minBy(squad.units.view.filter(_ != unit))(_.pixelDistanceCenter(unit.pixel))
    val joiningAlly  = ! suicide && squad.fightConsensus && nearestAlly.exists(a => SquadRally.shouldJoinAlly(unit.tileDistanceCenter(a.pixel)))
    if (joiningAlly) nearestAlly.map(_.pixel) else None
  }

  // March dressing: a leader running ahead of the pack holds position so
  // the line arrives together instead of trickling in. Waiting zeroes the
  // stuck counter (tryingToMove is false), so Unstick stays out of it.
  // Reavers never hold: a slow hull waiting ahead of a stalled pack
  // freezes next to the enemy, so it walks back to the pack instead.
  private def dressLeg(squad: Squad, unit: FriendlyUnitInfo, meanDistToGoalTiles: Double, suicide: Boolean): (Boolean, Boolean) = {
    val waiting      = ! suicide && squad.fightConsensus && MarchDress.shouldWait(unit.tileDistanceCenter(squad.vicinity), meanDistToGoalTiles)
    val reaverToArmy = Protoss.Reaver(unit) && MarchDress.shouldWait(unit.tileDistanceCenter(squad.vicinity), meanDistToGoalTiles)
    (waiting && ! reaverToArmy, reaverToArmy)
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
