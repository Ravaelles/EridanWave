package Micro.Actions.Combat.Decisioning

import Micro.Actions.Combat.Decisioning.CrucialAirSnipe._
import org.scalatest.funsuite.AnyFunSuite

class CrucialAirSnipeSuite extends AnyFunSuite {

  private def mark(distTiles: Double, kind: VictimKind, totalHealth: Double): Mark =
    Mark(distTiles, kind, totalHealth)

  test("a nearer scourge beats a closer carrier") {
    val marks = Seq(
      mark(distTiles = 2.0, Carrier, 300.0),
      mark(distTiles = 5.0, Scourge, 35.0))
    assert(CrucialAirSnipe.pick(marks).contains(1))
  }

  test("the nearest scourge goes first") {
    val marks = Seq(
      mark(distTiles = 5.0, Scourge, 35.0),
      mark(distTiles = 2.0, Scourge, 35.0))
    assert(CrucialAirSnipe.pick(marks).contains(1))
  }

  test("capitals go weakest-first in fixed order") {
    assert(CrucialAirSnipe.pick(Seq(
      mark(3.0, Wraith, 100.0),
      mark(3.0, Battlecruiser, 200.0),
      mark(3.0, Vessel, 150.0),
      mark(3.0, Carrier, 300.0))).contains(3))
    assert(CrucialAirSnipe.pick(Seq(
      mark(3.0, Carrier, 300.0),
      mark(3.0, Carrier, 150.0))).contains(1))
  }

  test("a Scourge past the range still gets sniped when it is coming at us") {
    assert(CrucialAirSnipe.inSnipeReach(inRange = true, isScourge = true, facingUs = true, beyondTiles = 0.0))
    assert(CrucialAirSnipe.inSnipeReach(inRange = false, isScourge = true, facingUs = true, beyondTiles = 1.5))
    assert(CrucialAirSnipe.inSnipeReach(inRange = false, isScourge = true, facingUs = true, beyondTiles = 0.5))
  }

  test("a leaving Scourge past the range is left alone") {
    assert( ! CrucialAirSnipe.inSnipeReach(inRange = false, isScourge = true, facingUs = false, beyondTiles = 0.5))
    assert( ! CrucialAirSnipe.inSnipeReach(inRange = false, isScourge = true, facingUs = true, beyondTiles = 1.51))
  }

  test("the snipe covers every grounded anti-air warrior") {
    testkit.TestKit.seedWorld()
    val covered = ProxyBwapi.UnitClasses.UnitClasses.all
      .filter(u => u.isWarrior && u.attacksAir && ! u.isFlyer)
      .map(_.toString)
      .toSet
    assert(covered == Set(
      "Marine",
      "Ghost",
      "Goliath",
      "Dragoon",
      "Archon",
      "Hydralisk",
      "Hero Jim Raynor Marine",
      "Hero Fenix Dragoon",
      "Hero Alexei Stukov",
      "Hero Alan Schezar",
      "Hero Tassadar Zeratul Archon",
      "Hero Infested Duran",
      "Hero Hunter Killer",
      "Hero Sarah Kerrigan",
      "Hero Samir Duran"))
  }

  test("capitals never get the reach bonus") {
    assert(CrucialAirSnipe.inSnipeReach(inRange = true, isScourge = false, facingUs = true, beyondTiles = 0.0))
    assert( ! CrucialAirSnipe.inSnipeReach(inRange = false, isScourge = false, facingUs = true, beyondTiles = 0.5))
  }

  test("no victims means no pick") {
    assert(CrucialAirSnipe.pick(Seq.empty).isEmpty)
  }
}
