package Information.Battles

import org.scalatest.funsuite.AnyFunSuite

class ArmyStrengthSuite extends AnyFunSuite {

  test("even strength scores a hundred") {
    assert(ArmyStrength.score(100.0, 100.0) == 100.0)
    assert(ArmyStrength.score(0.0, 0.0) == 100.0)
  }

  test("no scouted enemy counts as even") {
    assert(ArmyStrength.score(50.0, 0.0) == 100.0)
    assert(ArmyStrength.score(50.0, -10.0) == 100.0)
  }

  test("double and half scale linearly") {
    assert(ArmyStrength.score(200.0, 100.0) == 200.0)
    assert(ArmyStrength.score(50.0, 100.0) == 50.0)
  }

  test("below one-thirty the attack squad breaks off") {
    assert(ArmyStrength.retreating(ours = 124.0, foe = 100.0))
    assert(ArmyStrength.retreating(ours = 50.0, foe = 100.0))
    assert( ! ArmyStrength.retreating(ours = 130.0, foe = 100.0))
    assert( ! ArmyStrength.retreating(ours = 200.0, foe = 100.0))
    assert( ! ArmyStrength.retreating(ours = 50.0, foe = 0.0))
  }
}
