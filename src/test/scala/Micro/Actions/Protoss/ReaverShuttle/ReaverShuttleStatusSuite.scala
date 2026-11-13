package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class ReaverShuttleStatusSuite extends AnyFunSuite {

  test("the oldest healthy pair raids the enemy base") {
    assert(ReaverShuttleStatus.decide(isOldestReaver = true, shuttleHealth01 = 1.0, reaverHealth01 = 1.0, enemyIsZerg = false) == ReaverShuttleStatus.RaidBase)
  }

  test("younger pairs support the main army") {
    assert(ReaverShuttleStatus.decide(isOldestReaver = false, shuttleHealth01 = 1.0, reaverHealth01 = 1.0, enemyIsZerg = false) == ReaverShuttleStatus.SupportArmy)
  }

  test("either hull below half life sends even the oldest pair to the army") {
    assert(ReaverShuttleStatus.decide(isOldestReaver = true, shuttleHealth01 = 0.49, reaverHealth01 = 1.0, enemyIsZerg = false) == ReaverShuttleStatus.SupportArmy)
    assert(ReaverShuttleStatus.decide(isOldestReaver = true, shuttleHealth01 = 1.0, reaverHealth01 = 0.49, enemyIsZerg = false) == ReaverShuttleStatus.SupportArmy)
    assert(ReaverShuttleStatus.decide(isOldestReaver = true, shuttleHealth01 = 0.5, reaverHealth01 = 0.5, enemyIsZerg = false) == ReaverShuttleStatus.RaidBase)
  }

  test("against Zerg every pair supports the army, never raids") {
    assert(ReaverShuttleStatus.decide(isOldestReaver = true, shuttleHealth01 = 1.0, reaverHealth01 = 1.0, enemyIsZerg = true) == ReaverShuttleStatus.SupportArmy)
    assert(ReaverShuttleStatus.decide(isOldestReaver = false, shuttleHealth01 = 1.0, reaverHealth01 = 1.0, enemyIsZerg = true) == ReaverShuttleStatus.SupportArmy)
  }
}
