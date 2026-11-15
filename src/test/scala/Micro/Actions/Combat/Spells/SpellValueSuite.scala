package Micro.Actions.Combat.Spells

import org.scalatest.funsuite.AnyFunSuite

class SpellValueSuite extends AnyFunSuite {

  test("confidence bottoms out at one") {
    assert(SpellValue.confidence(0.0) == 1.0)
    assert(SpellValue.confidence(1.0) == 1.0)
    assert(SpellValue.confidence(-0.5) == 2.0)
  }

  test("danger pays the hull only under threat") {
    assert(SpellValue.danger(threatened = false, 100.0, 10.0) == 1.0)
    assert(SpellValue.danger(threatened = true, 100.0, 50.0) == 2.0)
    assert(SpellValue.danger(threatened = true, 100.0, 100.0) == 1.5)
  }

  test("a partial pack scales the crowd down") {
    assert(SpellValue.crowd(0) == 0.0)
    assert(SpellValue.crowd(1) == 1.0 / 3.0)
    assert(SpellValue.crowd(3) == 1.0)
    assert(SpellValue.crowd(30) == 1.0)
  }

  test("the chain multiplies every knob") {
    assert(SpellValue.of(10.0, 1.0, 1.0, 1.0, 1.0) == 10.0)
    assert(SpellValue.of(10.0, -2.0, 1.0, 1.0, 1.0) == -20.0)
    assert(SpellValue.of(10.0, 0.0, 2.0, 2.0, 2.0) == 0.0)
  }
}
