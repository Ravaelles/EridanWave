package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class PairArbiterSuite extends AnyFunSuite {

  private def decide(
    loaded: Boolean = false,
    mateOnFoot: Boolean = true,
    mateDistTiles: Double = Double.PositiveInfinity,
    reaverFoeTiles: Double = Double.PositiveInfinity,
    reaverGunTiles: Double = 8.0,
    reaverShotReady: Boolean = false,
    shuttlePressured: Boolean = false,
    reaverDoomed: Boolean = false,
    raid: Boolean = false): PairArbiter.PairDirective =
    PairArbiter.decide(
      loaded,
      mateOnFoot,
      mateDistTiles,
      reaverFoeTiles,
      reaverGunTiles,
      reaverShotReady,
      shuttlePressured,
      reaverDoomed,
      raid)

  test("a pressured loaded pair flees with the hull aboard") {
    val out = decide(loaded = true, shuttlePressured = true, raid = true)
    assert(out.shuttle == PairArbiter.Flee)
    assert(out.reaver == PairArbiter.RideAlong)
  }

  test("a doomed hull boards a close ride while the empty hull flees") {
    val out = decide(
      mateOnFoot = true,
      mateDistTiles = 1.0,
      shuttlePressured = true,
      reaverDoomed = true)
    assert(out.shuttle == PairArbiter.Flee)
    assert(out.reaver == PairArbiter.BoardRide)
  }

  test("a fighting mate is fetched, and fights until the ride arrives") {
    val out = decide(mateDistTiles = 30.0, reaverFoeTiles = 5.0)
    assert(out.shuttle == PairArbiter.FetchMate)
    assert(out.reaver == PairArbiter.FightOnFoot)
  }

  test("a loaded shot with a foe in reach holds instead of trading") {
    val out = decide(mateDistTiles = 30.0, reaverFoeTiles = 5.0, reaverShotReady = true)
    assert(out.shuttle == PairArbiter.FetchMate)
    assert(out.reaver == PairArbiter.HoldShoot)
  }

  test("past gun reach holding is waiting, so the hull fights on foot") {
    val out = decide(
      mateDistTiles = 30.0,
      reaverFoeTiles = PairArbiter.fightTiles,
      reaverGunTiles = 8.0,
      reaverShotReady = true)
    assert(out.reaver == PairArbiter.FightOnFoot)
  }

  test("a ride in reach is guarded while the hull boards") {
    val out = decide(mateDistTiles = 1.0, reaverFoeTiles = 20.0)
    assert(out.shuttle == PairArbiter.GuardMate)
    assert(out.reaver == PairArbiter.BoardRide)
  }

  test("a quiet mate is fetched while it walks to the ride") {
    val out = decide(mateDistTiles = 30.0, reaverFoeTiles = 20.0)
    assert(out.shuttle == PairArbiter.FetchMate)
    assert(out.reaver == PairArbiter.WalkToRide)
  }

  test("a loaded pair follows its status") {
    assert(decide(loaded = true, raid = true).shuttle == PairArbiter.RaidCarry)
    assert(decide(loaded = true, raid = false).shuttle == PairArbiter.FollowArmy)
    assert(decide(loaded = true).reaver == PairArbiter.RideAlong)
  }

  test("no mate means a post for the ride and the army for the hull") {
    val out = decide(mateOnFoot = false)
    assert(out.shuttle == PairArbiter.AwaitPost)
    assert(out.reaver == PairArbiter.WalkToArmy)
  }
}
