package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class RepairerSnipeSuite extends AnyFunSuite {

  test("weak units hold the line instead of chasing repairers") {
    assert( ! RepairerSnipe.shouldSnipe(actorHp = 79, buildingHp = 500))
    assert( ! RepairerSnipe.shouldSnipe(actorHp = 0, buildingHp = 500))
  }

  test("nearly dead buildings get finished, not diverted from") {
    assert( ! RepairerSnipe.shouldSnipe(actorHp = 200, buildingHp = 29))
    assert( ! RepairerSnipe.shouldSnipe(actorHp = 80, buildingHp = 0))
  }

  test("healthy units tasked on live buildings snipe the repairer") {
    assert(RepairerSnipe.shouldSnipe(actorHp = 80, buildingHp = 30))
    assert(RepairerSnipe.shouldSnipe(actorHp = 400, buildingHp = 1500))
  }

  test("repairers count as hittable a step past normal reach") {
    assert(RepairerSnipe.inChaseRange(distTiles = 5.5, ownRangeTiles = 4.0))
    assert(RepairerSnipe.inChaseRange(distTiles = 4.0, ownRangeTiles = 4.0))
    assert( ! RepairerSnipe.inChaseRange(distTiles = 5.51, ownRangeTiles = 4.0))
  }
}
