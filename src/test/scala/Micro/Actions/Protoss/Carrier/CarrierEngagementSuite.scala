package Micro.Actions.Protoss.Carrier

import org.scalatest.funsuite.AnyFunSuite

class CarrierEngagementSuite extends AnyFunSuite {

  import CarrierEngagement._

  private def margins(
      hasTarget          : Boolean = true,
      brawling           : Boolean = false,
      tankHunt           : Boolean = false,
      interceptorsActive : Boolean = true,
      chasingAlready     : Boolean = true,
      safetyTiles        : Double = 2.0,
      targetDistTiles    : Double = 5.0): CarrierMargins =
    CarrierMargins(hasTarget, brawling, tankHunt, interceptorsActive, chasingAlready, safetyTiles, targetDistTiles)

  test("no target warms interceptors up") {
    assert(decide(margins(hasTarget = false)) == WarmUp)
  }

  test("a fifty-interceptor wing never plays leash games") {
    assert(decide(margins(brawling = true, safetyTiles = -5.0)) == Brawl)
  }

  test("the tank hunt outranks the leash states") {
    assert(decide(margins(tankHunt = true, safetyTiles = -5.0)) == TankHunt)
  }

  test("cold interceptors launch instead of chasing") {
    assert(decide(margins(interceptorsActive = false)) == Launch)
  }

  test("chase with impunity or a target leaving the beat") {
    assert(decide(margins(chasingAlready = false, safetyTiles = 2.0)) == Chase)
    assert(decide(margins(chasingAlready = false, safetyTiles = 0.0, targetDistTiles = 9.1)) == Chase)
    assert(decide(margins(chasingAlready = true, safetyTiles = 2.0)) == Attack)
  }

  test("attack with impunity inside the beat") {
    assert(decide(margins(safetyTiles = 0.1)) == Attack)
  }

  test("a pressured Carrier opens the leash inside eight tiles") {
    assert(decide(margins(safetyTiles = 0.0, targetDistTiles = 7.9)) == OpenLeash)
    assert(decide(margins(safetyTiles = -1.0, targetDistTiles = 7.9)) == OpenLeash)
  }

  test("otherwise it holds the leash") {
    assert(decide(margins(safetyTiles = 0.0, targetDistTiles = 8.0)) == HoldLeash)
    assert(decide(margins(safetyTiles = -1.0, targetDistTiles = 9.0)) == HoldLeash)
  }

  test("priority follows the historical order") {
    assert(decide(margins(hasTarget = false, brawling = true)) == WarmUp)
    assert(decide(margins(brawling = true, tankHunt = true)) == Brawl)
    assert(decide(margins(tankHunt = true, interceptorsActive = false)) == TankHunt)
  }
}
