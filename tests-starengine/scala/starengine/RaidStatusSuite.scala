package starengine

import Micro.Actions.Protoss.ReaverShuttle.{PairArbiter, PairRaidStatus, ReaverShuttleStatus}
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Raid status memory, step by step: a loaded pair fights with wounds
 * drifting across the health lines. The pilot calls the shipped
 * PairRaidStatus.update on the same plain facts production reads, then
 * feeds the status into the shipped arbiter exactly like the game seam
 * does - and the order must never flap RaidCarry/FollowArmy inside the
 * band. The old sharp 0.5 line flipped the pair on every wound crossing
 * (shields regenerate, damage jumps); the 0.5-out / 0.65-back deadband
 * holds the assignment instead.
 */
class RaidStatusSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Raid.name + StarEngineTags.Reaver.name + StarEngineTags.Shuttle.name) }

  private def orderFor(raid: Boolean): PairArbiter.ShuttleOrder =
    PairArbiter.decide(
      loaded = true,
      mateOnFoot = false,
      mateDistTiles = Double.PositiveInfinity,
      reaverFoeTiles = Double.PositiveInfinity,
      reaverGunTiles = 0.0,
      reaverShotReady = false,
      shuttlePressured = false,
      reaverDoomed = false,
      raid = raid,
      baitRide = false).shuttle

  test("a raiding pair holds through the wound band, then supports") {
    val wounds = Seq(0.7, 0.6, 0.55, 0.62, 0.7, 0.6, 0.49, 0.5, 0.6)
    val statuses = wounds.map(w => PairRaidStatus.update(1, isOldestReaver = true, w, w))
    val orders = statuses.map(s => orderFor(s == ReaverShuttleStatus.RaidBase))
    println(s"DIAG raid statuses=${statuses.map(_.toString).mkString(",")} orders=${orders.map(_.label).mkString(",")}")
    assert(statuses.take(6).forall(_ == ReaverShuttleStatus.RaidBase), "band should hold the raid")
    assert(statuses(6) == ReaverShuttleStatus.SupportArmy, "below half the pair supports")
    assert(statuses(7) == ReaverShuttleStatus.SupportArmy, "half is not healed")
    assert(statuses(8) == ReaverShuttleStatus.SupportArmy, "0.6 without a fresh start stays home")
    assert(orders.take(6).forall(_ == PairArbiter.RaidCarry), "order flapped inside the band")
    assert(orders.drop(6).forall(_ == PairArbiter.FollowArmy), "order did not follow the support verdict")
  }

  test("a supporting pair climbs back out past the return line") {
    PairRaidStatus.update(2, isOldestReaver = true, 0.4, 0.4)
    assert(PairRaidStatus.update(2, isOldestReaver = true, 0.6, 0.6) == ReaverShuttleStatus.SupportArmy)
    assert(PairRaidStatus.update(2, isOldestReaver = true, 0.65, 0.65) == ReaverShuttleStatus.RaidBase)
  }

  test("hulls keep independent bits") {
    assert(PairRaidStatus.update(3, isOldestReaver = true, 1.0, 1.0) == ReaverShuttleStatus.RaidBase)
    assert(PairRaidStatus.update(4, isOldestReaver = true, 0.4, 0.4) == ReaverShuttleStatus.SupportArmy)
    assert(PairRaidStatus.update(3, isOldestReaver = true, 0.6, 0.6) == ReaverShuttleStatus.RaidBase)
  }
}
