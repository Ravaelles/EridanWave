package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class CarrierTankHuntSuite extends AnyFunSuite {

  test("a wing with interceptors always dives tanks in range") {
    assert(CarrierTankHunt.shouldHunt(tankNear = true, totalInterceptors = 10, enemyAANear = 5))
    assert(CarrierTankHunt.shouldHunt(tankNear = true, totalInterceptors = 48, enemyAANear = 0))
  }

  test("a toothless wing with AA cover stands down") {
    assert( ! CarrierTankHunt.shouldHunt(tankNear = true, totalInterceptors = 9, enemyAANear = 2))
    assert( ! CarrierTankHunt.shouldHunt(tankNear = true, totalInterceptors = 1, enemyAANear = 6))
  }

  test("a toothless wing without AA still goes") {
    assert(CarrierTankHunt.shouldHunt(tankNear = true, totalInterceptors = 9, enemyAANear = 1))
    assert(CarrierTankHunt.shouldHunt(tankNear = true, totalInterceptors = 1, enemyAANear = 0))
  }

  test("a naked wing never dives") {
    assert( ! CarrierTankHunt.shouldHunt(tankNear = true, totalInterceptors = 0, enemyAANear = 0))
    assert( ! CarrierTankHunt.shouldHunt(tankNear = true, totalInterceptors = 0, enemyAANear = 3))
  }

  test("no tank in range means no hunt") {
    assert( ! CarrierTankHunt.shouldHunt(tankNear = false, totalInterceptors = 48, enemyAANear = 0))
  }
}
