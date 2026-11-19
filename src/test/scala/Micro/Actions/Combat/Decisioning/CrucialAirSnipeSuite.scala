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

  test("casters and support follow the capitals in fixed order") {
    assert(CrucialAirSnipe.pick(Seq(
      mark(3.0, HighTemplar, 40.0),
      mark(3.0, DarkTemplar, 120.0),
      mark(3.0, Reaver, 200.0),
      mark(3.0, Observer, 40.0),
      mark(3.0, Wraith, 100.0))).contains(4))
    assert(CrucialAirSnipe.pick(Seq(
      mark(3.0, HighTemplar, 40.0),
      mark(3.0, DarkTemplar, 120.0))).contains(1))
    assert(CrucialAirSnipe.pick(Seq(
      mark(3.0, Observer, 40.0),
      mark(3.0, Reaver, 200.0))).contains(0))
  }

  test("a Scourge past the range still gets sniped when it is coming at us") {
    assert(CrucialAirSnipe.inSnipeReach(Scourge, inRange = true, inMargin = false, facingUs = true))
    assert(CrucialAirSnipe.inSnipeReach(Scourge, inRange = false, inMargin = true, facingUs = true))
  }

  test("a leaving Scourge past the range is left alone") {
    assert( ! CrucialAirSnipe.inSnipeReach(Scourge, inRange = false, inMargin = true, facingUs = false))
    assert( ! CrucialAirSnipe.inSnipeReach(Scourge, inRange = false, inMargin = false, facingUs = true))
  }

  test("a visible Observer a tile and a half past the guns gets walked down") {
    assert(CrucialAirSnipe.inSnipeReach(Observer, inRange = false, inMargin = true, facingUs = true))
    assert(CrucialAirSnipe.inSnipeReach(Observer, inRange = false, inMargin = true, facingUs = false))
    assert( ! CrucialAirSnipe.inSnipeReach(Observer, inRange = false, inMargin = false, facingUs = true))
  }

  test("slow capitals get walked down inside the margin, fast runners do not") {
    assert(CrucialAirSnipe.inSnipeReach(Carrier, inRange = false, inMargin = true, facingUs = false))
    assert(CrucialAirSnipe.inSnipeReach(Wraith, inRange = false, inMargin = true, facingUs = true))
    assert( ! CrucialAirSnipe.inSnipeReach(Wraith, inRange = false, inMargin = true, facingUs = false))
    assert(CrucialAirSnipe.inSnipeReach(Wraith, inRange = true, inMargin = false, facingUs = false))
  }

  test("a Dark Templar is dueled on terms: warm gun, room for ranged hulls") {
    assert(CrucialAirSnipe.duelsDarkTemplar(cooldownLeft = 0, melee = false, distTiles = 2.6))
    assert(CrucialAirSnipe.duelsDarkTemplar(cooldownLeft = 4, melee = false, distTiles = 10.0))
    assert(CrucialAirSnipe.duelsDarkTemplar(cooldownLeft = 4, melee = true, distTiles = 0.5))
    assert( ! CrucialAirSnipe.duelsDarkTemplar(cooldownLeft = 5, melee = false, distTiles = 10.0))
    assert( ! CrucialAirSnipe.duelsDarkTemplar(cooldownLeft = 5, melee = true, distTiles = 0.5))
    assert( ! CrucialAirSnipe.duelsDarkTemplar(cooldownLeft = 0, melee = false, distTiles = 2.59))
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

  test("only fast runners lose the margin when they flee") {
    assert(CrucialAirSnipe.inSnipeReach(Carrier, inRange = true, inMargin = false, facingUs = true))
    assert( ! CrucialAirSnipe.chasesAway(Carrier))
    assert( ! CrucialAirSnipe.chasesAway(Observer))
    assert(CrucialAirSnipe.chasesAway(Scourge))
    assert(CrucialAirSnipe.chasesAway(Wraith))
  }

  test("no victims means no pick") {
    assert(CrucialAirSnipe.pick(Seq.empty).isEmpty)
  }
}
