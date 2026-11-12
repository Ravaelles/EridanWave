package Information.Battles.Types

import org.scalatest.funsuite.AnyFunSuite

class JudgmentModifiersSuite extends AnyFunSuite {

  test("a lone static counts its bare value") {
    assert(JudgmentModifiers.guardedValue(100.0, 0.0) == 100.0)
  }

  test("a screened static counts fifty percent extra") {
    assert(JudgmentModifiers.guardedValue(100.0, 100.0) == 150.0)
    assert(JudgmentModifiers.guardedValue(100.0, 50.0) == 125.0)
  }

  test("a single defender does not screen a static") {
    assert( ! JudgmentModifiers.isGuarded(0))
    assert( ! JudgmentModifiers.isGuarded(1))
    assert(JudgmentModifiers.isGuarded(2))
    assert(JudgmentModifiers.isGuarded(9))
  }
}
