package Micro.Targeting.FiltersRequired

import org.scalatest.funsuite.AnyFunSuite

class TargetFilterScoutSuite extends AnyFunSuite {

  test("outside a Protoss mirror everything is legal") {
    assert(TargetFilterScout.allowsVictim(isScout = false, enemyIsProtoss = true, targetIsCarrier = false, mateNear = false))
    assert(TargetFilterScout.allowsVictim(isScout = true, enemyIsProtoss = false, targetIsCarrier = false, mateNear = false))
  }

  test("inside the mirror only escorted Carriers pass") {
    assert(TargetFilterScout.allowsVictim(isScout = true, enemyIsProtoss = true, targetIsCarrier = true, mateNear = true))
    assert( ! TargetFilterScout.allowsVictim(isScout = true, enemyIsProtoss = true, targetIsCarrier = true, mateNear = false))
    assert( ! TargetFilterScout.allowsVictim(isScout = true, enemyIsProtoss = true, targetIsCarrier = false, mateNear = true))
  }

  test("the wingmate band is five tiles") {
    assert(TargetFilterScout.wingmateTiles == 5.0)
  }
}
