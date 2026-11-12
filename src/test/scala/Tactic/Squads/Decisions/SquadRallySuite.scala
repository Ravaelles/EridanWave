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

  test("slow hulls hold the line five tiles further out") {
    assert(SquadRally.rallyRadiusFor(slow = false) == SquadRally.rallyRadiusPixels)
    assert(SquadRally.rallyRadiusFor(slow = true) == SquadRally.rallyRadiusPixels + 5 * 32)
    // 200px out rallies a normal unit but not a Reaver on the long leash.
    assert(SquadRally.isRallying(Pixel(1000, 1200), centroid, SquadRally.rallyRadiusFor(slow = false)))
    assert( ! SquadRally.isRallying(Pixel(1000, 1200), centroid, SquadRally.rallyRadiusFor(slow = true)))
    // Past the long leash even a Reaver rejoins.
    assert(SquadRally.isRallying(Pixel(1000, 1300), centroid, SquadRally.rallyRadiusFor(slow = true)))
    assert(SquadRally.rallyFor(Pixel(1000, 1200), centroid, terminus, SquadRally.rallyRadiusFor(slow = true)) == terminus)
  }

  test("drifters at six tenths close ranks") {
    assert(SquadRally.shouldJoinAlly(0.6))
    assert(SquadRally.shouldJoinAlly(5.0))
    assert( ! SquadRally.shouldJoinAlly(0.59))
    assert( ! SquadRally.shouldJoinAlly(0.0))
  }
}
