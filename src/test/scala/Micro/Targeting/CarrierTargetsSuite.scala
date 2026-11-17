package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class CarrierTargetsSuite extends AnyFunSuite {

  private def mark(
    distTiles: Double = 5.0,
    isTurret: Boolean = false,
    isGoliath: Boolean = false,
    isSiegedTank: Boolean = false,
    isUnsiegedTank: Boolean = false,
    isMarine: Boolean = false,
    isBunker: Boolean = false,
    isMedic: Boolean = false,
    isBuilderWorker: Boolean = false,
    isWorker: Boolean = false): CarrierTargets.Mark =
    CarrierTargets.Mark(distTiles, isTurret, isGoliath, isSiegedTank, isUnsiegedTank, isMarine, isBunker, isMedic, isBuilderWorker, isWorker)

  test("the ladder runs turret, goliath, tanks, marine, bunker, medic, builders, workers") {
    assert(CarrierTargets.tier(mark(isSiegedTank = true)) == 0)
    assert(CarrierTargets.tier(mark(isUnsiegedTank = true)) == 1)
    assert(CarrierTargets.tier(mark(isGoliath = true)) == 2)
    assert(CarrierTargets.tier(mark(isTurret = true)) == 3)
    assert(CarrierTargets.tier(mark(isMarine = true)) == 4)
    assert(CarrierTargets.tier(mark(isBunker = true)) == 5)
    assert(CarrierTargets.tier(mark(isMedic = true)) == 6)
    assert(CarrierTargets.tier(mark(isBuilderWorker = true)) == 7)
    assert(CarrierTargets.tier(mark(isWorker = true)) == 8)
    assert(CarrierTargets.tier(mark()) == 9)
  }

  test("a sieged tank outranks marines and workers, unconditionally") {
    val out = CarrierTargets.pick(Seq(
      mark(distTiles = 1.0, isWorker = true),
      mark(distTiles = 2.0, isMarine = true),
      mark(distTiles = 9.0, isSiegedTank = true)))
    assert(out.contains(2))
  }

  test("tanks outrank goliaths outrank turrets at any distance") {
    val out = CarrierTargets.pick(Seq(
      mark(distTiles = 1.0, isMarine = true),
      mark(distTiles = 2.0, isTurret = true),
      mark(distTiles = 3.0, isGoliath = true),
      mark(distTiles = 9.0, isSiegedTank = true)))
    assert(out.contains(3))
  }

  test("nearest inside the best tier wins, empty picks nothing") {
    val out = CarrierTargets.pick(Seq(
      mark(distTiles = 9.0, isMarine = true),
      mark(distTiles = 3.0, isMarine = true)))
    assert(out.contains(1))
    assert(CarrierTargets.pick(Seq.empty).isEmpty)
  }

  test("builders outrank idle workers") {
    val out = CarrierTargets.pick(Seq(
      mark(distTiles = 1.0, isWorker = true),
      mark(distTiles = 8.0, isBuilderWorker = true)))
    assert(out.contains(1))
  }
}
