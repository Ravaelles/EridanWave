package Information.Battles

import org.scalatest.funsuite.AnyFunSuite

class ApplyEarlyGamePressurePenaltySuite extends AnyFunSuite {

  private def adjust(
    strength: Double = 100.0,
    units: Int = 10,
    protoss: Boolean = true,
    dragoons: Boolean = true): Double =
    ApplyEarlyGamePressurePenalty.adjust(strength, units, protoss, dragoons)

  test("a small mirror force reads twenty percent weaker") {
    assert(adjust(strength = 100.0, units = 6) == 80.0)
    assert(adjust(strength = 200.0, units = 0) == 160.0)
  }

  test("grown armies and other matchups read whole") {
    assert(adjust(strength = 100.0, units = 7) == 100.0)
    assert(adjust(strength = 100.0, units = 20) == 100.0)
    assert(adjust(strength = 100.0, units = 6, protoss = false) == 100.0)
    assert(adjust(strength = 100.0, units = 6, dragoons = false) == 100.0)
  }
}
