package Placement.Walls

import Mathematics.Points.Tile
import org.scalatest.funsuite.AnyFunSuite

class WallScoringSuite extends AnyFunSuite {

  test("a gap flanked by two buildings costs nothing") {
    assert(WallScoring.gapPenalty(adjacentBuildings = 2, gapFartherThanWall = false) == 0)
    assert(WallScoring.gapPenalty(adjacentBuildings = 3, gapFartherThanWall = true) == 0)
  }

  test("an unflanked gap at the near end costs less than one on the far side") {
    val near = WallScoring.gapPenalty(adjacentBuildings = 1, gapFartherThanWall = false)
    val far  = WallScoring.gapPenalty(adjacentBuildings = 1, gapFartherThanWall = true)
    assert(near == 1200)
    assert(far == 400)
    assert(far < near)
  }

  test("alignment is half the tighter of the two axes") {
    // The tighter axis decides: 4 tiles of spread in y beat 10 in x.
    assert(WallScoring.alignment(Seq(10, 14, 20), Seq(100, 100, 104)) == 2.0)
    assert(WallScoring.alignment(Seq(0, 0), Seq(0, 10)) == 0.0)
  }

  test("a wall is horizontal when its buildings spread further in x") {
    assert(WallScoring.isHorizontal(Seq(0, 20), Seq(0, 4)))
    assert( ! WallScoring.isHorizontal(Seq(0, 4), Seq(0, 20)))
  }

  test("inner edges are the sides facing away from our origin") {
    val horizontal = Seq(
      WallPlacement.Rect(10, 10, 3, 3),
      WallPlacement.Rect(13, 11, 3, 3))
    // Origin above the wall: the inner edge is each building's bottom.
    assert(WallScoring.insideEdgeSpread(horizontal, horizontal = true, origin = Tile(12, 0)) == 1)
    // Origin below: the inner edge flips to each building's top.
    assert(WallScoring.insideEdgeSpread(horizontal, horizontal = true, origin = Tile(12, 40)) == 1)
  }

  test("a flush wall has no inside edge spread") {
    val flush = Seq(WallPlacement.Rect(10, 10, 3, 3), WallPlacement.Rect(13, 10, 3, 3))
    assert(WallScoring.insideEdgeSpread(flush, horizontal = true, origin = Tile(12, 0)) == 0)
  }

  test("a wall missing hall margin is penalized until six tiles") {
    val comfortable = WallScoring.score(perimeter = 0, alignment = 0, insideEdgeSpread = 0, hallMargin = 6, gapPenalty = 0, rampDistance = 0)
    val tight       = WallScoring.score(perimeter = 0, alignment = 0, insideEdgeSpread = 0, hallMargin = 3, gapPenalty = 0, rampDistance = 0)
    assert(comfortable == 0.0)
    assert(tight == 3 * WallScoring.hallMarginWeight)
  }

  test("perimeter and ramp distance pull in opposite directions") {
    val score = WallScoring.score(perimeter = 10, alignment = 0, insideEdgeSpread = 0, hallMargin = 6, gapPenalty = 0, rampDistance = 10)
    assert(score == 10 * WallScoring.perimeterWeight - 10 * WallScoring.distanceWeight)
  }
}
