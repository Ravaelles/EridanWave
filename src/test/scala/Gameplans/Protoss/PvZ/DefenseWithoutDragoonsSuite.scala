package Gameplans.Protoss.PvZ

import org.scalatest.funsuite.AnyFunSuite

class DefenseWithoutDragoonsSuite extends AnyFunSuite {

  test("weak Dragoonless vs Zerg defends") {
    assert(DefenseWithoutDragoons.shouldDefend(enemyIsZerg = true, dragoons = 0, ours = 124.0, foe = 100.0))
    assert(DefenseWithoutDragoons.shouldDefend(enemyIsZerg = true, dragoons = 0, ours = 50.0, foe = 100.0))
  }

  test("Dragoons present keep the posture") {
    assert( ! DefenseWithoutDragoons.shouldDefend(enemyIsZerg = true, dragoons = 1, ours = 50.0, foe = 100.0))
    assert( ! DefenseWithoutDragoons.shouldDefend(enemyIsZerg = true, dragoons = 10, ours = 50.0, foe = 100.0))
  }

  test("even strength or other enemies keep the posture") {
    assert( ! DefenseWithoutDragoons.shouldDefend(enemyIsZerg = true, dragoons = 0, ours = 200.0, foe = 100.0))
    assert( ! DefenseWithoutDragoons.shouldDefend(enemyIsZerg = true, dragoons = 0, ours = 50.0, foe = 0.0))
    assert( ! DefenseWithoutDragoons.shouldDefend(enemyIsZerg = false, dragoons = 0, ours = 50.0, foe = 100.0))
  }
}
