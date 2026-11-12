package Information.Battles.Prediction.Simulation

import org.scalatest.funsuite.AnyFunSuite

class SimulationSplashSuite extends AnyFunSuite {

  private def divisor(
    distance: Double,
    outer: Double = 40,
    inner: Double = 10,
    median: Double = 25,
    line: Boolean = false,
    air: Boolean = false,
    burrowed: Boolean = false
  ) = SimulationSplash.splashDivisor(distance, outer, inner, median, line, air, burrowed)

  test("a hit outside the outer radius does no splash") {
    assert(divisor(40.1) == 0)
    assert(divisor(40.0) == 4)
  }

  test("the ring ladder is full, half, quarter damage") {
    assert(divisor(10.0) == 1)
    assert(divisor(25.0) == 2)
    assert(divisor(25.1) == 4)
  }

  test("burrowed units ignore partial splash") {
    assert(divisor(25.0, burrowed = true) == 0)
    assert(divisor(30.0, burrowed = true) == 0)
    // Full damage inside the inner radius still lands.
    assert(divisor(10.0, burrowed = true) == 1)
  }

  test("air splash keeps its inner radius") {
    assert(divisor(10.0, air = true) == 2)
    assert(divisor(25.0, air = true) == 2)
    assert(divisor(30.0, air = true) == 4)
  }

  test("a travelling spine hits only its own footprint") {
    assert(divisor(5.0, line = true) == 1)
    assert(divisor(15.0, line = true) == 0)
  }

  test("bounds cover the whole segment plus the radius") {
    // Segment (0,0)-(320,0), radius 64: x from -64 to 384, y from -64 to 64.
    val (xMin, xMax, yMin, yMax) = SimulationSplash.splashBounds(0, 0, 320, 0, 64, 256, 256)
    assert(xMin == 0)
    assert(xMax == 12)
    assert(yMin == 0)
    assert(yMax == 2)
  }

  test("bounds are clipped to the map") {
    val (xMin, xMax, yMin, yMax) = SimulationSplash.splashBounds(32, 32, 32, 32, 512, 64, 64)
    assert(xMin == 0)
    assert(yMin == 0)
    // 32 + 512 = 544 pixels, still inside a 64-tile map.
    assert(xMax == 17)
    assert(yMax == 17)
  }
}
