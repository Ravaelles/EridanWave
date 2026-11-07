package Information.Battles

import org.scalatest.funsuite.AnyFunSuite

class KillLedgerSuite extends AnyFunSuite {

  private def fresh(): Unit = KillLedger.reset()

  test("killing a unit counts the kill and its mineral+gas price") {
    fresh()
    KillLedger.record(friendly = false, building = false, cost = 150)
    assert(KillLedger.killed == 1)
    assert(KillLedger.killedBuildings == 0)
    assert(KillLedger.killedResources == 150)
    assert(KillLedger.lost == 0)
  }

  test("killing a building counts both the kill and the building") {
    fresh()
    KillLedger.record(friendly = false, building = true, cost = 400)
    assert(KillLedger.killed == 1)
    assert(KillLedger.killedBuildings == 1)
    assert(KillLedger.killedResources == 400)
  }

  test("losing our unit counts symmetrically") {
    fresh()
    KillLedger.record(friendly = true, building = false, cost = 125)
    assert(KillLedger.lost == 1)
    assert(KillLedger.killed == 0)
    assert(KillLedger.lostResources == 125)
  }

  test("balance is kills minus losses in resources") {
    fresh()
    KillLedger.record(friendly = false, building = false, cost = 300)
    KillLedger.record(friendly = true, building = false, cost = 100)
    assert(KillLedger.balance == 200)
  }

  test("a losing trade shows a negative balance") {
    fresh()
    KillLedger.record(friendly = true, building = true, cost = 400)
    assert(KillLedger.balance == -400)
  }

  test("reset zeroes every counter") {
    KillLedger.record(friendly = false, building = true, cost = 999)
    KillLedger.record(friendly = true, building = false, cost = 999)
    KillLedger.reset()
    assert(KillLedger.killed == 0)
    assert(KillLedger.killedBuildings == 0)
    assert(KillLedger.lost == 0)
    assert(KillLedger.lostBuildings == 0)
    assert(KillLedger.killedResources == 0)
    assert(KillLedger.lostResources == 0)
    assert(KillLedger.balance == 0)
  }
}
