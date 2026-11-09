package Tactic.Tactics.Scouting.Monitor

import MonitorWatch.WatchMemory
import org.scalatest.funsuite.AnyFunSuite

class MonitorWatchSuite extends AnyFunSuite {

  private val alive   = (id: Int) => id != 7
  private val allLive = (id: Int) => true

  test("fresh memory is not grounded") {
    val (memory, grounded) =
      MonitorWatch.refresh(WatchMemory(), now = 100, alive = allLive, observerCount = 1)
    assert( ! grounded)
    assert(memory.lastFrame == 100)
  }

  test("an eye eaten on duty grounds monitoring below three Observers") {
    val (assigned, _) =
      MonitorWatch.refresh(WatchMemory(), now = 100, alive = allLive, observerCount = 2)
    val (_, grounded) =
      MonitorWatch.refresh(assigned.copy(lastIds = Set(7)), now = 200, alive = alive, observerCount = 2)
    assert(grounded)
  }

  test("three Observers override grounding") {
    val (_, grounded) =
      MonitorWatch.refresh(WatchMemory(lastFrame = 100, lastIds = Set(7)), now = 200, alive = alive, observerCount = 3)
    assert( ! grounded)
  }

  test("a new game clears the memory") {
    val (_, grounded) =
      MonitorWatch.refresh(WatchMemory(lastFrame = 20000, lastIds = Set(7), deathFrame = 20000), now = 10, alive = alive, observerCount = 1)
    assert( ! grounded)
  }
}
