package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class ReaverShuttleStatusSuite extends AnyFunSuite {

  test("a healed oldest pair starts raiding") {
    assert(ReaverShuttleStatus.shouldRaid(isOldestReaver = true, shuttleHealth01 = 1.0, reaverHealth01 = 1.0))
    assert(ReaverShuttleStatus.shouldRaid(isOldestReaver = true, shuttleHealth01 = 0.65, reaverHealth01 = 0.65))
  }

  test("a fresh pair inside the band stays home") {
    assert( ! ReaverShuttleStatus.shouldRaid(isOldestReaver = true, shuttleHealth01 = 0.64, reaverHealth01 = 1.0))
    assert( ! ReaverShuttleStatus.shouldRaid(isOldestReaver = true, shuttleHealth01 = 0.5, reaverHealth01 = 0.5))
  }

  test("younger pairs never start") {
    assert( ! ReaverShuttleStatus.shouldRaid(isOldestReaver = false, shuttleHealth01 = 1.0, reaverHealth01 = 1.0))
    assert(ReaverShuttleStatus.shouldSupport(isOldestReaver = false, shuttleHealth01 = 1.0, reaverHealth01 = 1.0))
  }

  test("either bleeding hull stops even the oldest pair") {
    assert(ReaverShuttleStatus.shouldSupport(isOldestReaver = true, shuttleHealth01 = 0.49, reaverHealth01 = 1.0))
    assert(ReaverShuttleStatus.shouldSupport(isOldestReaver = true, shuttleHealth01 = 1.0, reaverHealth01 = 0.49))
  }

  test("a raiding pair holds through the band") {
    assert( ! ReaverShuttleStatus.shouldSupport(isOldestReaver = true, shuttleHealth01 = 0.5, reaverHealth01 = 0.5))
    assert( ! ReaverShuttleStatus.shouldSupport(isOldestReaver = true, shuttleHealth01 = 0.64, reaverHealth01 = 1.0))
  }
}
