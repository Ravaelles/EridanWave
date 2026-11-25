package Micro.Actions

import ProxyBwapi.Races.{Protoss => Races, Terran => RacesTerran, Zerg => RacesZerg}
import ProxyBwapi.UnitClasses.UnitClass
import org.scalatest.funsuite.AnyFunSuite

class UnitSubsetsSuite extends AnyFunSuite {

  private val zealot: UnitClass = Races.Zealot
  private val dragoon: UnitClass = Races.Dragoon
  private val templar: UnitClass = Races.DarkTemplar
  private val corsair: UnitClass = Races.Corsair
  private val scout: UnitClass = Races.Scout
  private val probe: UnitClass = Races.Probe
  private val shuttle: UnitClass = Races.Shuttle
  private val observer: UnitClass = Races.Observer
  private val hydra: UnitClass = RacesZerg.Hydralisk
  private val tank: UnitClass = RacesTerran.SiegeTankUnsieged

  test("AllUnits keeps every class, so adopted gates change nothing") {
    Seq(zealot, dragoon, templar, corsair, probe, shuttle, observer, hydra, tank).foreach(u =>
      assert(UnitSubsets.AllUnits.contains(u)))
  }

  test("ground, air and armed split the tree without overlap surprises") {
    assert(UnitSubsets.GroundUnits.contains(zealot))
    assert(UnitSubsets.GroundUnits.contains(probe))
    assert( ! UnitSubsets.GroundUnits.contains(corsair))
    assert(UnitSubsets.AirUnits.contains(corsair))
    assert(UnitSubsets.AirUnits.contains(observer))
    assert( ! UnitSubsets.AirUnits.contains(dragoon))
    assert(UnitSubsets.ArmedUnits.contains(dragoon))
    assert(UnitSubsets.ArmedUnits.contains(hydra))
    assert(UnitSubsets.ArmedUnits.contains(probe))
    assert( ! UnitSubsets.ArmedUnits.contains(shuttle))
    assert( ! UnitSubsets.ArmedUnits.contains(observer))
    assert(UnitSubsets.Workers.contains(probe))
    assert( ! UnitSubsets.Workers.contains(zealot))
  }

  test("the pack line excludes raiders, transports and eyes") {
    assert(UnitSubsets.LineArmy.contains(zealot))
    assert(UnitSubsets.LineArmy.contains(dragoon))
    assert(UnitSubsets.LineArmy.contains(hydra))
    assert(UnitSubsets.LineArmy.contains(tank))
    assert( ! UnitSubsets.LineArmy.contains(templar))
    assert( ! UnitSubsets.LineArmy.contains(corsair))
    assert( ! UnitSubsets.LineArmy.contains(scout))
    assert( ! UnitSubsets.LineArmy.contains(probe))
    assert( ! UnitSubsets.LineArmy.contains(shuttle))
    assert( ! UnitSubsets.LineArmy.contains(observer))
  }
}
