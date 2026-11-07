package Mathematics

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class PointsSuite extends AnyFunSuite {

  test("squared distance is exact Pythagoras") {
    assert(Pixel(0, 0).pixelDistanceSquared(Pixel(3, 4)) == 25)
    assert(Pixel(0, 0).pixelDistanceSquared(Pixel(0, 0)) == 0)
  }

  test("Brood War distance is exact on axes and close on diagonals") {
    assert(Pixel(0, 0).pixelDistance(Pixel(5, 0)) == 5.0)
    assert(Pixel(0, 0).pixelDistance(Pixel(0, 0)) == 0.0)
    val diagonal = Pixel(0, 0).pixelDistance(Pixel(3, 4))
    assert(math.abs(diagonal - 5.0) < 0.2, diagonal)
  }

  test("Chebyshev distance is the king-move count") {
    assert(Pixel(0, 0).pixelDistanceChebyshev(Pixel(3, 4)) == 4)
    assert(Pixel(2, 2).pixelDistanceChebyshev(Pixel(2, 2)) == 0)
  }

  test("pixel arithmetic adds component-wise") {
    assert(Pixel(1, 2) + Pixel(10, 20) == Pixel(11, 22))
  }
}
