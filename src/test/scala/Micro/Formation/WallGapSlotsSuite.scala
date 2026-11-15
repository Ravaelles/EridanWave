package Micro.Formation

import Mathematics.Points.Pixel
import Micro.Formation.WallGapGeometry.{Body, Box}
import org.scalatest.funsuite.AnyFunSuite

class WallGapSlotsSuite extends AnyFunSuite {

  private val ling = Body(5, 5, 5, 5)
  private val zealot = Body(15, 15, 16, 16)
  private val slots = new WallGapSlots(ling, (_, _, _) => true, (_, _) => true)

  private val gap = WallGapGeometry.between(
    Box(0, 0, 32, 64),
    Box(96, 0, 128, 64),
    Pixel(64, 500),
    None,
    Vector.empty,
    _ => false,
    ling,
    (_, _, _) => true).get

  test("a corridor offers sealing candidates ordered by exposure") {
    val found = slots.candidates(gap, zealot)
    assert(found.nonEmpty)
    assert(found.map(_.exposure) == found.map(_.exposure).sorted)
    assert(found.forall(_.exposure >= 0))
  }

  test("reserves sit behind the sealing front") {
    val front = slots.candidates(gap, zealot).take(2)
    val reserves = slots.reinforcementCandidates(gap, zealot, front)
    assert(reserves.nonEmpty)
    assert(reserves.forall( ! _.front))
  }
}
