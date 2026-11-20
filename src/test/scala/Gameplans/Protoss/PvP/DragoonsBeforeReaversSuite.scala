package Gameplans.Protoss.PvP

import org.scalatest.funsuite.AnyFunSuite

class DragoonsBeforeReaversSuite extends AnyFunSuite {

  test("the first Reaver waits for fifteen Dragoons") {
    assert( ! DragoonsBeforeReavers.allowReavers(dragoons = 0, enemyCannonKnown = false))
    assert( ! DragoonsBeforeReavers.allowReavers(dragoons = 14, enemyCannonKnown = false))
    assert(DragoonsBeforeReavers.allowReavers(dragoons = 15, enemyCannonKnown = false))
    assert(DragoonsBeforeReavers.allowReavers(dragoons = 30, enemyCannonKnown = false))
  }

  test("a scouted Cannon answers without the backbone") {
    assert(DragoonsBeforeReavers.allowReavers(dragoons = 0, enemyCannonKnown = true))
  }
}
