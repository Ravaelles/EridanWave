package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class ProtossWallInSuite extends AnyFunSuite {

  test("three buildings on the choke in time means a wall") {
    assert(ProtossWallIn.detected(buildingCount = 3, frame = 0))
    assert(ProtossWallIn.detected(buildingCount = 5, frame = 10000))
    assert( ! ProtossWallIn.detected(buildingCount = 2, frame = 0))
    assert( ! ProtossWallIn.detected(
      buildingCount = 3,
      frame = ProtossWallIn.detectFrames + 1))
  }

  test("one building left or minute nine over ends the mission") {
    assert(ProtossWallIn.released(buildingCount = 1, frame = 0))
    assert(ProtossWallIn.released(buildingCount = 0, frame = 0))
    assert(ProtossWallIn.released(
      buildingCount = 3,
      frame = ProtossWallIn.detectFrames + 1))
    assert( ! ProtossWallIn.released(buildingCount = 2, frame = 0))
  }

  test("the mission latches past the entry and releases on the kill") {
    def latch(count: Int, frame: Int): Boolean =
      ProtossWallIn.missionLatched(count, frame)
    assert( ! latch(count = 2, frame = 0))
    assert(latch(count = 3, frame = 0))
    assert(latch(count = 2, frame = 0))
    assert( ! latch(count = 1, frame = 0))
  }

  test("fifteen wound steps off, thirteen steps back") {
    def latch(id: Int, wound: Int): Boolean =
      ProtossWallIn.woundLatched(id, wound)
    assert( ! latch(id = 11, wound = 14))
    assert(latch(id = 11, wound = 15))
    assert(latch(id = 11, wound = 14))
    assert( ! latch(id = 11, wound = 13))
    assert( ! latch(id = 11, wound = 0))
  }

  test("a maxed line or a bank fights wounded") {
    assert(ProtossWallIn.overrideNow(supplyUsed = 180, minerals = 0))
    assert(ProtossWallIn.overrideNow(supplyUsed = 0, minerals = 1000))
    assert( ! ProtossWallIn.overrideNow(supplyUsed = 179, minerals = 999))
  }

  test("holders hold, fighters fight, the bank overrules") {
    assert(ProtossWallIn.shouldFightWall(withdrawing = false, supplyUsed = 0, minerals = 0))
    assert( ! ProtossWallIn.shouldFightWall(withdrawing = true, supplyUsed = 0, minerals = 0))
    assert(ProtossWallIn.shouldFightWall(withdrawing = true, supplyUsed = 180, minerals = 0))
    assert(ProtossWallIn.shouldFightWall(withdrawing = true, supplyUsed = 0, minerals = 1000))
  }
}
