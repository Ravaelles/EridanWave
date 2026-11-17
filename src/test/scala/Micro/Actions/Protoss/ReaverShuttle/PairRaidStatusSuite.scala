package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class PairRaidStatusSuite extends AnyFunSuite {

  test("a healed oldest pair raids, a bleeding one supports") {
    assert(PairRaidStatus.update(1, isOldestReaver = true, ownHealth01 = 1.0, mateHealth01 = 1.0) == ReaverShuttleStatus.RaidBase)
    assert(PairRaidStatus.update(2, isOldestReaver = true, ownHealth01 = 0.4, mateHealth01 = 1.0) == ReaverShuttleStatus.SupportArmy)
    assert(PairRaidStatus.update(3, isOldestReaver = false, ownHealth01 = 1.0, mateHealth01 = 1.0) == ReaverShuttleStatus.SupportArmy)
  }

  test("the band holds the assignment instead of flapping") {
    PairRaidStatus.update(10, isOldestReaver = true, ownHealth01 = 0.7, mateHealth01 = 0.7)
    assert(PairRaidStatus.update(10, isOldestReaver = true, ownHealth01 = 0.6, mateHealth01 = 0.6) == ReaverShuttleStatus.RaidBase)
    assert(PairRaidStatus.update(10, isOldestReaver = true, ownHealth01 = 0.55, mateHealth01 = 0.64) == ReaverShuttleStatus.RaidBase)
    assert(PairRaidStatus.update(10, isOldestReaver = true, ownHealth01 = 0.49, mateHealth01 = 0.9) == ReaverShuttleStatus.SupportArmy)
    assert(PairRaidStatus.update(10, isOldestReaver = true, ownHealth01 = 0.6, mateHealth01 = 0.6) == ReaverShuttleStatus.SupportArmy)
    assert(PairRaidStatus.update(10, isOldestReaver = true, ownHealth01 = 0.7, mateHealth01 = 0.7) == ReaverShuttleStatus.RaidBase)
  }
}
