package Micro.Formation

import Mathematics.Points.Pixel
import Micro.Formation.WallGapGeometry.Body
import org.scalatest.funsuite.AnyFunSuite

class WallGapNavigationSuite extends AnyFunSuite {

  private val body = Body(8, 8, 8, 8)
  private val inside = Pixel(80, 80)

  // Open grid except a blocked bar across the middle.
  private def grid(barred: Boolean): WallGapNavigation =
    new WallGapNavigation(
      body,
      0,
      0,
      20,
      20,
      (b, p, _) => ! barred || p.x < 80 || p.x >= 120 || p.y < 0 || p.y >= 200,
      inside)

  test("open ground reaches everywhere and waypoints straight") {
    val nav = grid(barred = false)
    assert(nav.reachable(Pixel(150, 150)))
    val from = Pixel(10, 10)
    val next = nav.waypoint(1, from, Pixel(150, 150))
    // On the straight segment, capped at one short move (Brood War
    // distance undercounts diagonals, so the cap is measured its way).
    assert(next != from)
    assert(next.x == next.y)
    assert(from.pixelDistance(next) <= 64.0)
  }

  test("a sealed pocket is unreachable, so the waypoint holds the goal") {
    val nav = grid(barred = true)
    assert( ! nav.reachable(Pixel(100, 100)))
    assert(nav.waypoint(1, Pixel(100, 100), Pixel(300, 100)) == Pixel(300, 100))
  }

  test("around a bar the waypoint advances in short moves") {
    val nav = grid(barred = true)
    val from = Pixel(40, 100)
    assert(nav.reachable(from))
    val next = nav.waypoint(1, from, Pixel(300, 100))
    assert(next != from)
    assert(from.pixelDistance(next) <= 64)
  }
}
