package Micro.Actions.Combat.Maneuvering

import org.scalatest.funsuite.AnyFunSuite

class WorkerThreatFleeSuite extends AnyFunSuite {

  test("three mutalisks make the pack") {
    assert(WorkerThreatFlee.mutaPack(3))
    assert(WorkerThreatFlee.mutaPack(9))
    assert( ! WorkerThreatFlee.mutaPack(2))
  }

  test("shelling counts inside tank range plus margin") {
    assert(WorkerThreatFlee.tankShelling(distTiles = 13.0, tankRangeTiles = 12.0))
    assert(WorkerThreatFlee.tankShelling(distTiles = 5.0, tankRangeTiles = 12.0))
    assert( ! WorkerThreatFlee.tankShelling(distTiles = 14.01, tankRangeTiles = 12.0))
    assert( ! WorkerThreatFlee.tankShelling(distTiles = 20.0, tankRangeTiles = 12.0))
  }

  test("a hurt miner flees a melee pack it cannot beat alone") {
    assert(WorkerThreatFlee.doomedVsMelee(totalHealth = 37, meleeNearby = 2, supported = false))
    assert(WorkerThreatFlee.doomedVsMelee(totalHealth = 1, meleeNearby = 5, supported = false))
  }

  test("a healthy miner holds, one rival is not a pack") {
    assert( ! WorkerThreatFlee.doomedVsMelee(totalHealth = 38, meleeNearby = 2, supported = false))
    assert( ! WorkerThreatFlee.doomedVsMelee(totalHealth = 37, meleeNearby = 1, supported = false))
  }

  test("a supported miner still swarms with the army") {
    assert( ! WorkerThreatFlee.doomedVsMelee(totalHealth = 37, meleeNearby = 2, supported = true))
    assert( ! WorkerThreatFlee.doomedVsMelee(totalHealth = 1, meleeNearby = 9, supported = true))
  }

  test("a dying miner runs from a single closing fighter") {
    assert(WorkerThreatFlee.doomedVsLoneFighter(totalHealth = 30, fighterClosing = true, supported = false))
    assert(WorkerThreatFlee.doomedVsLoneFighter(totalHealth = 1, fighterClosing = true, supported = false))
  }

  test("a healthier miner holds, a distant fighter does not count") {
    assert( ! WorkerThreatFlee.doomedVsLoneFighter(totalHealth = 31, fighterClosing = true, supported = false))
    assert( ! WorkerThreatFlee.doomedVsLoneFighter(totalHealth = 30, fighterClosing = false, supported = false))
    assert( ! WorkerThreatFlee.doomedVsLoneFighter(totalHealth = 30, fighterClosing = true, supported = true))
  }
}
