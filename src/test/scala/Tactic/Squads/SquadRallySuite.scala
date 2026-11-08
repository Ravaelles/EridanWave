package Tactic.Squads

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
}
