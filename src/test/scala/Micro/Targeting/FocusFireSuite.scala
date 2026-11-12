package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class FocusFireSuite extends AnyFunSuite {

  test("shots to kill rounds up and never returns zero") {
    assert(FocusFire.shotsToKill(100.0, 20.0) == 5)
    assert(FocusFire.shotsToKill(101.0, 20.0) == 6)
    assert(FocusFire.shotsToKill(5.0, 20.0) == 1)
  }

  test("no damage per shot cannot kill anything") {
    assert(FocusFire.shotsToKill(100.0, 0.0) == Int.MaxValue)
  }

  test("an empty candidate set has no focus target") {
    assert(FocusFire.choose(Seq.empty).isEmpty)
  }

  test("the target the most shooters can hit wins") {
    val chosen = FocusFire.choose(Seq(
      FocusFire.Candidate(shotsToKill = 1, reach = 3, packDistanceTiles = 1.0, id = 1),
      FocusFire.Candidate(shotsToKill = 5, reach = 9, packDistanceTiles = 9.0, id = 2),
      FocusFire.Candidate(shotsToKill = 2, reach = 5, packDistanceTiles = 2.0, id = 3)))

    assert(chosen.map(_.id).contains(2))
  }

  test("equal reach resolves by volleys, then pack distance, then id") {
    val byVolleys = FocusFire.choose(Seq(
      FocusFire.Candidate(shotsToKill = 3, reach = 4, packDistanceTiles = 1.0, id = 1),
      FocusFire.Candidate(shotsToKill = 1, reach = 4, packDistanceTiles = 9.0, id = 2)))
    assert(byVolleys.map(_.id).contains(2))

    val byDistance = FocusFire.choose(Seq(
      FocusFire.Candidate(shotsToKill = 2, reach = 4, packDistanceTiles = 4.5, id = 1),
      FocusFire.Candidate(shotsToKill = 2, reach = 4, packDistanceTiles = 2.5, id = 2)))
    assert(byDistance.map(_.id).contains(2))

    val byId = FocusFire.choose(Seq(
      FocusFire.Candidate(shotsToKill = 2, reach = 4, packDistanceTiles = 3.0, id = 9),
      FocusFire.Candidate(shotsToKill = 2, reach = 4, packDistanceTiles = 3.0, id = 4)))
    assert(byId.map(_.id).contains(4))
  }
}
