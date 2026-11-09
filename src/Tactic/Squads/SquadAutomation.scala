package Tactic.Squads

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.Pixel
import Micro.Formation.{Formation, FormationStyleDisengage, FormationStyleGuard, Formations}
import Micro.Targeting.ArmyTargeting
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.?

import scala.collection.mutable.ArrayBuffer

object SquadAutomation {

  ////////////////
  // Formations //
  ////////////////

  def form(squad: Squad, from: Pixel, to: Pixel): ArrayBuffer[Formation] = {
    val output: ArrayBuffer[Formation] = ArrayBuffer.empty
    // If advancing, give a formation for forward movement.
    // March as soon as enemies are known en route (Stardust: form the line
    // before contact), not only after the squad voted to fight.
    if (SquadMarch.shouldFormMarch(squad.fightConsensus, squad.battleEnemies.nonEmpty)) {
      output += Formations.march(squad, to)
    }
    // Always include a disengagey formation for units that want to retreat/kite
    if (squad.centroidKey.zone == squad.homeConsensus.zone && With.scouting.enemyThreatOrigin.zone != squad.homeConsensus.zone) {
      output += Formations.guard(squad, Some(squad.homeConsensus))
    } else {
      output += Formations.disengage(squad)
    }
    output
  }

  def send(squad: Squad, defaultRedoubt: Option[Pixel] = None): Unit = {
    sendUnits(squad, squad.unintended, defaultRedoubt)
  }

  def sendUnits(squad: Squad, units: Iterable[FriendlyUnitInfo], defaultReturn: Option[Pixel] = None): Unit = {
    // Common march reference: the pack mean distance to the squad goal, in tiles.
    // Leaders ahead of it wait (MarchDress); laggards march or rally back.
    lazy val meanDistToGoalTiles = Maff.mean(units.view.map(_.pixelDistanceCenter(squad.vicinity))) / 32.0
    units.foreach(unit => {
      lazy val finalTravel = getTravel(unit, squad, defaultReturn)
      lazy val finalReturn = getReturn(unit, squad, defaultReturn)
      // Straggler rally (Stardust): a unit lagging behind the marching pack
      // rejoins the pack centroid first instead of walking alone to its slot.
      // Returning units keep their own slots - they are meant to disperse.
      lazy val pack     = ?(unit.flying, squad.centroidAir, squad.centroidGround)
      lazy val rallying = squad.fightConsensus && SquadRally.isRallying(unit.pixel, pack)
      lazy val finalMarch = ?(rallying, pack, finalTravel)
      // March dressing: a leader running ahead of the pack holds position so
      // the line arrives together instead of trickling in. Waiting zeroes the
      // stuck counter (tryingToMove is false), so Unstick stays out of it.
      lazy val waiting = squad.fightConsensus && MarchDress.shouldWait(unit.pixelDistanceCenter(squad.vicinity) / 32.0, meanDistToGoalTiles)
      lazy val finalTerminus = ?(waiting, unit.pixel, finalMarch)
      Debugging.CherryVis.CherryVis.decision(unit.id,
        ?(squad.fightConsensus, ?(waiting, "Waiting for pack", ?(rallying, "Rallying to pack centroid", "Marching to formation slot")), "Returning to slot"))
      unit.intend(squad)
        .setTerminus(?(squad.fightConsensus, finalTerminus, finalReturn))
        .setRedoubt(finalReturn)
    })
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

  //////////////////////
  // Full automation! //
  //////////////////////

  def targetAndSend(squad: Squad): Unit = targetAndSend(squad, from = squad.homeConsensus, to = squad.vicinity)
  def targetAndSend(squad: Squad, from: Pixel, to: Pixel): Unit = {
    squad.setTargets(ArmyTargeting.rankedEnRoute(squad, ?(squad.fightConsensus, squad.vicinity, squad.homeConsensus)))
    send(squad, defaultRedoubt = Some(from))
  }

  def targetFormAndSend(squad: Squad): Unit = targetFormAndSend(squad, from = squad.homeConsensus, to = squad.vicinity)
  def targetFormAndSend(squad: Squad, from: Pixel, to: Pixel): Unit = {
    squad.setTargets(ArmyTargeting.rankedEnRoute(squad, ?(squad.fightConsensus, squad.vicinity, squad.homeConsensus)))
    squad.formations = form(squad, from, to)
    send(squad, defaultRedoubt = Some(from))
  }

  def formAndSend(squad: Squad): Unit = formAndSend(squad, from = squad.homeConsensus, to = squad.vicinity)
  def formAndSend(squad: Squad, from: Pixel, to: Pixel): Unit = {
    squad.formations = form(squad, from, to)
    send(squad, defaultRedoubt = Some(from))
  }
}
