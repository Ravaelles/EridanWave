package Gameplans.Protoss.PvZ.policy

import org.scalatest.funsuite.AnyFunSuite

class DefenseWithoutDragoonsSuite extends AnyFunSuite {

  test("weak Dragoonless vs Zerg defends") {
    assert(DefenseWithoutDragoons.shouldDefend(enemyIsZerg = true, dragoons = 0, zealots = 6, ours = 124.0, foe = 100.0))
    assert(DefenseWithoutDragoons.shouldDefend(enemyIsZerg = true, dragoons = 0, zealots = 2, ours = 50.0, foe = 100.0))
  }

  test("a real Zealot force keeps the posture") {
    assert( ! DefenseWithoutDragoons.shouldDefend(enemyIsZerg = true, dragoons = 0, zealots = 7, ours = 50.0, foe = 100.0))
    assert( ! DefenseWithoutDragoons.shouldDefend(enemyIsZerg = true, dragoons = 0, zealots = 12, ours = 50.0, foe = 100.0))
  }

  test("Dragoons present keep the posture") {
    assert( ! DefenseWithoutDragoons.shouldDefend(enemyIsZerg = true, dragoons = 1, zealots = 0, ours = 50.0, foe = 100.0))
    assert( ! DefenseWithoutDragoons.shouldDefend(enemyIsZerg = true, dragoons = 10, zealots = 0, ours = 50.0, foe = 100.0))
  }

  test("strength boundary sits at 165") {
    assert(DefenseWithoutDragoons.shouldDefend(enemyIsZerg = true, dragoons = 0, zealots = 6, ours = 165.0, foe = 100.0))
    assert( ! DefenseWithoutDragoons.shouldDefend(enemyIsZerg = true, dragoons = 0, zealots = 6, ours = 166.0, foe = 100.0))
  }

  test("even strength or other enemies keep the posture") {
    assert( ! DefenseWithoutDragoons.shouldDefend(enemyIsZerg = true, dragoons = 0, zealots = 6, ours = 200.0, foe = 100.0))
    assert( ! DefenseWithoutDragoons.shouldDefend(enemyIsZerg = true, dragoons = 0, zealots = 6, ours = 50.0, foe = 0.0))
    assert( ! DefenseWithoutDragoons.shouldDefend(enemyIsZerg = false, dragoons = 0, zealots = 0, ours = 50.0, foe = 100.0))
  }
}
