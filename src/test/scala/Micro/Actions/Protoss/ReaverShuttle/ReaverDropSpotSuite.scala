package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class ReaverDropSpotSuite extends AnyFunSuite {

  private def mark(static: Double, mobile: Double, home: Double): ReaverDropSpot.Candidate =
    ReaverDropSpot.Candidate(0.0, 0.0, static, mobile, home)

  test("the ring holds eight spots eight and a half tiles out") {
    val spots = ReaverDropSpot.ring((10.0, 10.0))
    assert(spots.size == 8)
    spots.foreach { case (x, y) =>
      val dist = Math.hypot(x - 10.0, y - 10.0)
      assert(dist > 8.49 && dist < 8.51)
    }
  }

  test("a spot needs static floor and mobile margin both") {
    assert(ReaverDropSpot.isValid(7.5, 4.0))
    assert(ReaverDropSpot.isValid(9.0, 10.0))
    assert( ! ReaverDropSpot.isValid(7.49, 10.0))
    assert( ! ReaverDropSpot.isValid(9.0, 3.99))
  }

  test("nearest-home valid spot wins, none when all break a margin") {
    assert(ReaverDropSpot.pick(Seq.empty).isEmpty)
    assert(ReaverDropSpot.pick(Seq(mark(5.0, 1.0, 1.0))).isEmpty)
    assert(ReaverDropSpot.pick(Seq(
      mark(8.5, 5.0, 30.0),
      mark(8.5, 5.0, 10.0),
      mark(8.5, 5.0, 20.0))).exists(_.distHomeTiles == 10.0))
  }
}
