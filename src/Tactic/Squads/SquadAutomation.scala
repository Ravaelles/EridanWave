package Tactic.Squads

import Mathematics.Points.Pixel
import Micro.Formation.Formation
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Tactic.Squads.Group.FriendlyUnitGroup

import scala.collection.mutable.ArrayBuffer

object SquadAutomation {

  ///////////////
  // Targeting //
  ///////////////

  def target(squad: Squad): Unit = SquadTargeting.target(squad)
  def target(squad: Squad, to: Pixel): Unit = SquadTargeting.target(squad, to)

  def targetRaid(squad: Squad): Unit = SquadTargeting.targetRaid(squad)
  def targetRaid(squad: Squad, to: Pixel): Unit = SquadTargeting.targetRaid(squad, to)

  def rankForArmy(squad: Squad, targets: Vector[UnitInfo]): Vector[UnitInfo] =
    SquadTargeting.rankForArmy(squad, targets)

  def unrankedEnRouteTo(group: FriendlyUnitGroup, to: Pixel): Set[UnitInfo] =
    SquadTargeting.unrankedEnRouteTo(group, to)

  def unrankedAround(group: FriendlyUnitGroup, to: Pixel): Vector[UnitInfo] =
    SquadTargeting.unrankedAround(group, to)

  def rankedEnRoute(squad: Squad): Seq[UnitInfo] = SquadTargeting.rankedEnRoute(squad)
  def rankedEnRoute(squad: Squad, goalAir: Pixel): Seq[UnitInfo] = SquadTargeting.rankedEnRoute(squad, goalAir)
  def rankedAround(squad: Squad): Seq[UnitInfo] = SquadTargeting.rankedAround(squad)
  def rankedAround(squad: Squad, goalAir: Pixel): Seq[UnitInfo] = SquadTargeting.rankedAround(squad, goalAir)

  ////////////////
  // Formations //
  ////////////////

  def form(squad: Squad, from: Pixel, to: Pixel): ArrayBuffer[Formation] =
    SquadForming.form(squad, from, to)

  def send(squad: Squad, defaultRedoubt: Option[Pixel] = None): Unit = {
    MarchOrders.sendUnits(squad, squad.unintended, defaultRedoubt)
  }

  def getReturn(unit: FriendlyUnitInfo, squad: Squad, defaultReturn: Option[Pixel] = None): Pixel =
    MarchOrders.getReturn(unit, squad, defaultReturn)

  def getTravel(unit: FriendlyUnitInfo, squad: Squad, defaultReturn: Option[Pixel] = None): Pixel =
    MarchOrders.getTravel(unit, squad, defaultReturn)

  //////////////////////
  // Full automation! //
  //////////////////////

  def targetAndSend(squad: Squad): Unit = targetAndSend(squad, from = squad.homeConsensus, to = squad.vicinity)
  def targetAndSend(squad: Squad, from: Pixel, to: Pixel): Unit = {
    target(squad)
    send(squad, defaultRedoubt = Some(from))
  }

  def targetFormAndSend(squad: Squad): Unit = targetFormAndSend(squad, from = squad.homeConsensus, to = squad.vicinity)
  def targetFormAndSend(squad: Squad, from: Pixel, to: Pixel): Unit = {
    target(squad)
    squad.formations = form(squad, from, to)
    send(squad, defaultRedoubt = Some(from))
  }

  def formAndSend(squad: Squad): Unit = formAndSend(squad, from = squad.homeConsensus, to = squad.vicinity)
  def formAndSend(squad: Squad, from: Pixel, to: Pixel): Unit = {
    squad.formations = form(squad, from, to)
    send(squad, defaultRedoubt = Some(from))
  }
}
