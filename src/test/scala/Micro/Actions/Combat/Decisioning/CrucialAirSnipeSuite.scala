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

  test("no victims means no pick") {
    assert(CrucialAirSnipe.pick(Seq.empty).isEmpty)
  }
}
