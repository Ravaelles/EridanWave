package Micro.Actions.Protoss.DarkTemplar

import org.scalatest.funsuite.AnyFunSuite

class SpiderMineSnipingSuite extends AnyFunSuite {

  import SpiderMineSniping._

  private def sighting(
      targetedUs        : Boolean = false,
      distPixels        : Double = 50.0,
      neighborsInRadius : Int = 1,
      visible           : Boolean = true,
      cloaked           : Boolean = false): MineSighting =
    MineSighting(targetedUs, distPixels, neighborsInRadius, visible, cloaked)

  test("a mine targeting us is answered at any distance") {
    assert(shouldSnipe(sighting(targetedUs = true, distPixels = 500.0)))
  }

  test("a lone mine in radius is answered") {
    assert(shouldSnipe(sighting(distPixels = 50.0, neighborsInRadius = 1)))
  }

  test("a mine out of radius is ignored unless it targets us") {
    assert( ! shouldSnipe(sighting(distPixels = 200.0, neighborsInRadius = 1)))
    assert( ! shouldSnipe(sighting(distPixels = 96.0, neighborsInRadius = 1)))
  }

  test("a clustered mine is not a lone snipe") {
    assert( ! shouldSnipe(sighting(distPixels = 50.0, neighborsInRadius = 2)))
    assert( ! shouldSnipe(sighting(distPixels = 50.0, neighborsInRadius = 0)))
  }

  test("an unseen or cloaked mine is never answered") {
    assert( ! shouldSnipe(sighting(targetedUs = true, visible = false)))
    assert( ! shouldSnipe(sighting(targetedUs = true, cloaked = true)))
  }

  test("the sidestep goes lateral, never radial") {
    val dt = new Mathematics.Points.Pixel(100, 100)
    val centroid = new Mathematics.Points.Pixel(100, 200)
    val left = sidestepPixel(dt, centroid, stepTiles = 4.0, goLeft = true)
    val right = sidestepPixel(dt, centroid, stepTiles = 4.0, goLeft = false)
    // Ray points north (away from the centroid below); lateral means east-west.
    assert(left.x < dt.x && math.abs(left.y - dt.y) < 4.0)
    assert(right.x > dt.x && math.abs(right.y - dt.y) < 4.0)
    // Full stride, not a nudge.
    assert(math.abs(left.x - dt.x) > 100)
  }

  test("coincident points hold instead of dividing by zero") {
    val dt = new Mathematics.Points.Pixel(100, 100)
    assert(sidestepPixel(dt, dt, stepTiles = 4.0, goLeft = true) == dt)
  }
}
