package Tactic.Squads.Decisions

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class SquadRallySuite extends AnyFunSuite {

  private val centroid = Pixel(1000, 1000)
  private val terminus = Pixel(3000, 3000)

  test("straggler beyond rally radius rejoins the centroid") {
    assert(SquadRally.rallyFor(Pixel(1000, 1200), centroid, terminus) == centroid)
  }

  test("unit inside rally radius keeps its terminus") {
    assert(SquadRally.rallyFor(Pixel(1050, 1050), centroid, terminus) == terminus)
  }

  test("unit exactly at the radius boundary keeps its terminus") {
    assert(SquadRally.rallyFor(Pixel(1000 + SquadRally.rallyRadiusPixels, 1000), centroid, terminus) == terminus)
  }

  test("unit sitting on the centroid keeps its terminus") {
    assert(SquadRally.rallyFor(centroid, centroid, terminus) == terminus)
  }

  test("isRallying mirrors the rally decision") {
    assert(SquadRally.isRallying(Pixel(1000, 1200), centroid))
    assert( ! SquadRally.isRallying(Pixel(1050, 1050), centroid))
  }

  test("drifters at six tenths close ranks") {
    assert(SquadRally.shouldJoinAlly(0.6))
    assert(SquadRally.shouldJoinAlly(5.0))
    assert( ! SquadRally.shouldJoinAlly(0.59))
    assert( ! SquadRally.shouldJoinAlly(0.0))
  }
}
