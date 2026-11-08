package Information.Geography

import org.scalatest.funsuite.AnyFunSuite

class ChokesSuite extends AnyFunSuite {

  test("central and wide beats central and narrow") {
    assert(Chokes.middleScoreOf(distancePixels = 200, radiusPixels = 96) <
      Chokes.middleScoreOf(distancePixels = 200, radiusPixels = 16))
  }

  test("nearby and narrow beats far and wide") {
    assert(Chokes.middleScoreOf(distancePixels = 64, radiusPixels = 16) <
      Chokes.middleScoreOf(distancePixels = 640, radiusPixels = 96))
  }
}
