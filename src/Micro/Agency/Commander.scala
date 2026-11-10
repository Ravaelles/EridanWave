package Micro.Agency

import Mathematics.Points.{Pixel, Tile}
import Micro.Coordination.Pushing.TrafficPriority
import ProxyBwapi.Techs.Tech
import ProxyBwapi.UnitClasses.UnitClass
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import ProxyBwapi.Upgrades.Upgrade

// Commander is responsible for issuing unit commands
// in a way that Brood War handles gracefully,
// while taking advantage of Brood War mechanics to optimize commands.
//
// The goal is for the rest of the code base to be blissfully unaware
// of Brood War's glitchy unit behavior.
//
// Structure: thin facade, one reason to change per delegate.
// Travel moves, Order corrects destinations, Engage attacks,
// Work gathers and builds, Transport ferries, Abilities toggle state,
// Core owns the shared order lifecycle.
//
object Commander {

  def doNothing(unit: FriendlyUnitInfo): Unit = CommanderMove.doNothing(unit)
  def stop(unit: FriendlyUnitInfo): Unit = CommanderMove.stop(unit)
  def hold(unit: FriendlyUnitInfo): Unit = CommanderMove.hold(unit)
  def attack(unit: FriendlyUnitInfo): Unit = CommanderEngage.attack(unit)
  def attackMove(unit: FriendlyUnitInfo): Unit = CommanderMove.attackMove(unit)
  def attackMove(unit: FriendlyUnitInfo, destination: Pixel): Unit = CommanderMove.attackMove(unit, destination)
  def patrol(unit: FriendlyUnitInfo): Unit = CommanderMove.patrol(unit)
  def patrol(unit: FriendlyUnitInfo, destination: Pixel): Unit = CommanderMove.patrol(unit, destination)
  def move(unit: FriendlyUnitInfo): Unit = CommanderMove.move(unit)
  def move(unit: FriendlyUnitInfo, tolerancePixels: Double): Unit = CommanderMove.move(unit, tolerancePixels)
  def move(unit: FriendlyUnitInfo, destination: Pixel, tolerancePixels: Double = 128): Unit = CommanderMove.move(unit, destination, tolerancePixels)
  def adjustDestination(unit: FriendlyUnitInfo, argTo: Pixel): Pixel = CommanderOrder.adjustDestination(unit, argTo)
  def rightClick(unit: FriendlyUnitInfo, target: UnitInfo): Unit = CommanderTransport.rightClick(unit, target)
  def useTech(unit: FriendlyUnitInfo, tech: Tech): Unit = CommanderEngage.useTech(unit, tech)
  def useTechOnUnit(unit: FriendlyUnitInfo, tech: Tech, target: UnitInfo): Unit = CommanderEngage.useTechOnUnit(unit, tech, target)
  def useTechOnPixel(unit: FriendlyUnitInfo, tech: Tech, target: Pixel): Unit = CommanderEngage.useTechOnPixel(unit, tech, target)
  def repair(unit: FriendlyUnitInfo, target: UnitInfo): Unit = CommanderWork.repair(unit, target)
  def returnCargo(unit: FriendlyUnitInfo): Unit = CommanderWork.returnCargo(unit)
  def gather(unit: FriendlyUnitInfo): Unit = CommanderWork.gather(unit)
  def build(unit: FriendlyUnitInfo, unitClass: UnitClass): Unit = CommanderWork.build(unit, unitClass)
  def build(unit: FriendlyUnitInfo, unitClass: UnitClass, tile: Tile): Unit = CommanderWork.build(unit, unitClass, tile)
  def tech(unit: FriendlyUnitInfo, tech: Tech): Unit = CommanderWork.tech(unit, tech)
  def upgrade(unit: FriendlyUnitInfo, upgrade: Upgrade): Unit = CommanderWork.upgrade(unit, upgrade)
  def cancel(unit: FriendlyUnitInfo): Unit = CommanderWork.cancel(unit)
  def rally(unit: FriendlyUnitInfo, pixel: Pixel): Unit = CommanderWork.rally(unit, pixel)
  def requestUnload(passenger: FriendlyUnitInfo): Unit = CommanderTransport.requestUnload(passenger)
  def unload(transport: FriendlyUnitInfo, passenger: UnitInfo): Unit = CommanderTransport.unload(transport, passenger)
  def addon(unit: FriendlyUnitInfo, unitClass: UnitClass): Unit = CommanderWork.addon(unit, unitClass)
  def buildScarab(unit: FriendlyUnitInfo, count: Int = 1): Unit = CommanderAbilities.buildScarab(unit, count)
  def buildInterceptor(unit: FriendlyUnitInfo): Unit = CommanderAbilities.buildInterceptor(unit)
  def cloak(unit: FriendlyUnitInfo, tech: Tech): Unit = CommanderAbilities.cloak(unit, tech)
  def decloak(unit: FriendlyUnitInfo, tech: Tech): Unit = CommanderAbilities.decloak(unit, tech)
  def burrow(unit: FriendlyUnitInfo): Unit = CommanderAbilities.burrow(unit)
  def unburrow(unit: FriendlyUnitInfo): Unit = CommanderAbilities.unburrow(unit)
  def lift(unit: FriendlyUnitInfo): Unit = CommanderAbilities.lift(unit)
  def land(unit: FriendlyUnitInfo, tile: Tile): Unit = CommanderAbilities.land(unit, tile)
  def defaultEscalation(unit: FriendlyUnitInfo): Unit = CommanderCore.defaultEscalation(unit)
  def pushAway(unit: FriendlyUnitInfo, to: Pixel, minimumPriority: TrafficPriority, radius: Double = 48): Unit = CommanderCore.pushAway(unit, to, minimumPriority, radius)
  def pushThrough(unit: FriendlyUnitInfo, to: Pixel, minimumPriority: TrafficPriority, maxDistance: Double = 96): Unit = CommanderCore.pushThrough(unit, to, minimumPriority, maxDistance)
  def sleep(unit: FriendlyUnitInfo, requiredDelay: Int = 2): Unit = CommanderCore.sleep(unit, requiredDelay)
}
