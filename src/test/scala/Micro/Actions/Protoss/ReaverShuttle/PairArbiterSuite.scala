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
    raid: Boolean = false,
    baitRide: Boolean = false,
    unloadOrdered: Boolean = false,
    unloadSpotReady: Boolean = false,
    boardLocked: Boolean = false,
    threatTiles: Double = Double.PositiveInfinity,
    longTrip: Boolean = false,
    wound01: Double = 0.0): PairArbiter.PairDirective =
    PairArbiter.decide(
      loaded,
      mateOnFoot,
      mateDistTiles,
      reaverFoeTiles,
      reaverGunTiles,
      reaverShotReady,
      shuttlePressured,
      reaverDoomed,
      raid,
      baitRide,
      unloadOrdered,
      unloadSpotReady,
      boardLocked,
      threatTiles,
      longTrip,
      wound01)

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

  test("a ride in reach is guarded while the hull boards for its trip") {
    val out = decide(mateDistTiles = 1.0, reaverFoeTiles = 20.0, longTrip = true)
    assert(out.shuttle == PairArbiter.GuardMate)
    assert(out.reaver == PairArbiter.BoardRide)
  }

  test("a quiet mate is fetched while it walks to the ride") {
    val out = decide(mateDistTiles = 30.0, reaverFoeTiles = 20.0)
    assert(out.shuttle == PairArbiter.FetchMate)
    assert(out.reaver == PairArbiter.WalkToRide)
  }

  test("an adjacent ride is not boarded for a short hop") {
    val out = decide(mateDistTiles = 1.0)
    assert(out.shuttle == PairArbiter.FetchMate)
    assert(out.reaver == PairArbiter.WalkToRide)
  }

  test("a real trip boards the adjacent ride") {
    val out = decide(mateDistTiles = 1.0, longTrip = true)
    assert(out.reaver == PairArbiter.BoardRide)
  }

  test("a loaded pair follows its status") {
    assert(decide(loaded = true, raid = true).shuttle == PairArbiter.RaidCarry)
    assert(decide(loaded = true, raid = false).shuttle == PairArbiter.FollowArmy)
    assert(decide(loaded = true).reaver == PairArbiter.RideAlong)
  }

  test("landing is an order: the band ready or the mission word puts the hull out") {
    assert(decide(loaded = true, unloadSpotReady = true).reaver == PairArbiter.UnloadHere)
    assert(decide(loaded = true, unloadOrdered = true).reaver == PairArbiter.UnloadHere)
    assert(decide(loaded = true, unloadOrdered = true).shuttle == PairArbiter.FollowArmy)
  }

  test("pressure keeps a ready hull aboard") {
    val out = decide(loaded = true, shuttlePressured = true, unloadSpotReady = true)
    assert(out.shuttle == PairArbiter.Flee)
    assert(out.reaver == PairArbiter.RideAlong)
  }

  test("landing flags mean nothing on foot") {
    val out = decide(unloadSpotReady = true, unloadOrdered = true)
    assert(out.reaver == PairArbiter.WalkToRide)
  }

  test("no mate means a post for the ride and the army for the hull") {
    val out = decide(mateOnFoot = false)
    assert(out.shuttle == PairArbiter.AwaitPost)
    assert(out.reaver == PairArbiter.WalkToArmy)
  }

  test("no pair cargo anywhere means bait, not hover") {
    val out = decide(mateOnFoot = false, baitRide = true)
    assert(out.shuttle == PairArbiter.Bait)
  }

  private def shuttleView(
    pos: (Double, Double) = (0.0, 0.0),
    squad: Option[Int] = Some(1),
    aboard: Boolean = false,
    threats: Seq[PairArbiter.ThreatView] = Seq.empty): PairArbiter.ShuttleView =
    PairArbiter.ShuttleView(pos, squad, aboard, threats)

  private def threat(dist: Double, range: Double = 5.0): PairArbiter.ThreatView =
    PairArbiter.ThreatView(dist, range)

  private def reaverView(
    pos: (Double, Double) = (30.0, 0.0),
    squad: Option[Int] = Some(1),
    loaded: Boolean = false,
    bond: Option[PairArbiter.ShuttleView] = None,
    wound01: Double = 0.0,
    unloadOrdered: Boolean = false,
    unloadSpotReady: Boolean = false,
    boardLocked: Boolean = false,
    threatTiles: Double = Double.PositiveInfinity,
    longTrip: Boolean = false,
    foes: Seq[PairArbiter.FoeView] = Seq.empty): PairArbiter.ReaverView =
    PairArbiter.ReaverView(pos, squad, loaded, bond, 1, activeScarab = false, 0, wound01, unloadOrdered, unloadSpotReady, boardLocked, threatTiles, longTrip, foes)

  test("resolve: an unbonded hull sees the free same-squad ride") {
    val ride = shuttleView()
    val out = PairArbiter.resolveReaver(reaverView(), Seq(ride), raid = false)
    assert(out.reaver == PairArbiter.WalkToRide)
    assert(out.shuttle == PairArbiter.FetchMate)
  }

  test("resolve: the live regression - no WalkToArmy while a free ride exists") {
    val ride = shuttleView(pos = (100.0, 0.0))
    val out = PairArbiter.resolveReaver(reaverView(pos = (0.0, 0.0)), Seq(ride), raid = false)
    assert(out.reaver != PairArbiter.WalkToArmy)
  }

  test("resolve: a ride of another squad is not ours to chase") {
    val ride = shuttleView(squad = Some(2))
    val out = PairArbiter.resolveReaver(reaverView(squad = Some(1)), Seq(ride), raid = false)
    assert(out.reaver == PairArbiter.WalkToArmy)
  }

  test("resolve: a loaded ride is not free") {
    val ride = shuttleView(aboard = true)
    val out = PairArbiter.resolveReaver(reaverView(), Seq(ride), raid = false)
    assert(out.reaver == PairArbiter.WalkToArmy)
  }

  test("resolve: the reservation wins over a nearer free ride") {
    val bonded = shuttleView(pos = (40.0, 0.0))
    val nearer = shuttleView(pos = (3.0, 0.0))
    val reaver = reaverView(bond = Some(bonded))
    val out = PairArbiter.resolveReaver(reaver, Seq(nearer, bonded), raid = false)
    // The nearer ride is inside rejoin reach: taking it would board.
    // The bond holds the hull on its own ride instead.
    assert(out.reaver == PairArbiter.WalkToRide)
  }

  test("resolve: a gun across the map is not pressure") {
    val ride = shuttleView(threats = Seq(threat(dist = 40.0)))
    val out = PairArbiter.resolveShuttle(ride, Seq.empty, raid = false, baitRide = true)
    assert(out.shuttle == PairArbiter.Bait)
  }

  test("resolve: a gun inside the band routs the pair") {
    val ride = shuttleView(threats = Seq(threat(dist = 10.0)), aboard = true)
    val out = PairArbiter.resolveShuttle(ride, Seq.empty, raid = false, baitRide = false)
    assert(out.shuttle == PairArbiter.Flee)
  }

  test("resolve: shuttle side fetches the unloaded mate") {
    val out = PairArbiter.resolveShuttle(shuttleView(), Seq(reaverView()), raid = false, baitRide = false)
    assert(out.shuttle == PairArbiter.FetchMate)
  }

  test("resolve: a loaded hull leaves on the landing band") {
    val out = PairArbiter.resolveReaver(
      reaverView(loaded = true, unloadSpotReady = true),
      Seq(shuttleView()),
      raid = false)
    assert(out.reaver == PairArbiter.UnloadHere)
  }

  test("resolve: a quiet crowded mill is not fetched and never boards") {
    val out = PairArbiter.resolveReaver(
      reaverView(boardLocked = true),
      Seq(shuttleView()),
      raid = false)
    assert(out.reaver == PairArbiter.FightOnFoot)
    val shuttle = PairArbiter.resolveShuttle(
      shuttleView(),
      Seq(reaverView(boardLocked = true)),
      raid = false,
      baitRide = false)
    assert(shuttle.shuttle == PairArbiter.AwaitPost)
  }

  test("resolve: doom rescue still boards past the quiet-crowd pin") {
    val foe = PairArbiter.FoeView(distTiles = 1.0, foeGunTiles = 5.0, reaverGunTiles = 8.0)
    val threat = PairArbiter.ThreatView(distTiles = 3.0, rangeTiles = 5.0)
    val out = PairArbiter.resolveReaver(
      reaverView(
        pos = (0.0, 0.0),
        bond = Some(shuttleView(pos = (1.0, 0.0), threats = Seq(threat))),
        wound01 = 0.8,
        boardLocked = true,
        foes = Seq(foe)),
      Seq(shuttleView(pos = (1.0, 0.0), threats = Seq(threat))),
      raid = false)
    assert(out.reaver == PairArbiter.BoardRide)
  }

  test("resolve: a quiet adjacent ride is not boarded for a short hop") {
    val out = PairArbiter.resolveReaver(
      reaverView(pos = (0.0, 0.0), bond = Some(shuttleView(pos = (1.0, 0.0)))),
      Seq(shuttleView(pos = (1.0, 0.0))),
      raid = false)
    assert(out.reaver == PairArbiter.WalkToRide)
  }

  test("resolve: a real trip boards the adjacent ride") {
    val out = PairArbiter.resolveReaver(
      reaverView(pos = (0.0, 0.0), bond = Some(shuttleView(pos = (1.0, 0.0))), longTrip = true),
      Seq(shuttleView(pos = (1.0, 0.0))),
      raid = false)
    assert(out.reaver == PairArbiter.BoardRide)
  }

  test("resolve: a threat at the hull boards it out") {
    val out = PairArbiter.resolveReaver(
      reaverView(pos = (0.0, 0.0), bond = Some(shuttleView(pos = (1.0, 0.0))), threatTiles = 1.0),
      Seq(shuttleView(pos = (1.0, 0.0))),
      raid = false)
    assert(out.reaver == PairArbiter.BoardRide)
  }
}
