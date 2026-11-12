package Lifecycle

import org.scalatest.funsuite.AnyFunSuite

class HitCounterSuite extends AnyFunSuite {

  test("a newcomer counts nothing, only its baseline is stored") {
    val (hits, next) = HitCounter.countHits(Map.empty, Seq((1, 100)))

    assert(hits == 0)
    assert(next == Map(1 -> 100))
  }

  test("a hit-point drop counts once per frame") {
    val (hits, next) = HitCounter.countHits(Map(1 -> 100), Seq((1, 80)))

    assert(hits == 1)
    assert(next == Map(1 -> 80))
  }

  test("unchanged and regenerated hit points count nothing") {
    assert(HitCounter.countHits(Map(1 -> 100), Seq((1, 100)))._1 == 0)
    assert(HitCounter.countHits(Map(1 -> 80), Seq((1, 100)))._1 == 0)
  }

  test("each damaged unit counts once, the dead leave the map") {
    val (hits, next) = HitCounter.countHits(
      Map(1 -> 100, 2 -> 100, 3 -> 100),
      Seq((1, 90), (2, 100), (4, 50)))

    assert(hits == 1)
    assert(next == Map(1 -> 90, 2 -> 100, 4 -> 50))
  }
}
