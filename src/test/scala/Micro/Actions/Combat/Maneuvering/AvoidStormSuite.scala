package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class AvoidStormSuite extends AnyFunSuite {

  test("the cell plus margin leaves, the outside holds") {
    assert(AvoidStorm.shouldLeave(3.0))
    assert(AvoidStorm.shouldLeave(4.0))
    assert( ! AvoidStorm.shouldLeave(4.01))
    assert( ! AvoidStorm.shouldLeave(9.0))
  }

  test("the escape runs straight away from the live cell") {
    val away = AvoidStorm.awayFrom(Pixel(3200, 3200), Pixel(3200, 3296))
    assert(away.x == 3200)
    assert(away.y < 3200)
  }
}
