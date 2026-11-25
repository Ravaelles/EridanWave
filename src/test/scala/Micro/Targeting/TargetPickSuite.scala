package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class TargetPickSuite extends AnyFunSuite {

  private def row(id: Int, score: Double, current: Boolean = false, legal: Boolean = true): TargetPick.Row =
    TargetPick.Row(id, score, current, legal)

  test("unranked strata take the first legal row, score and incumbency ignored") {
    val rows = Seq(row(1, 99.0), row(2, 1.0, current = true), row(3, 50.0))
    assert(TargetPick.pick(rows, rankByScore = false).contains(1))
    assert(TargetPick.pick(Seq(row(1, 1.0, legal = false), row(2, 1.0)), rankByScore = false).contains(2))
    assert(TargetPick.pick(Seq(row(1, 1.0, legal = false)), rankByScore = false).isEmpty)
  }

  test("ranked strata take the max legal score, ties keep ladder order") {
    val rows = Seq(row(1, 10.0), row(2, 30.0), row(3, 30.0), row(4, 20.0, legal = false))
    assert(TargetPick.pick(rows, rankByScore = true).contains(2))
    assert(TargetPick.pick(Seq.empty, rankByScore = true).isEmpty)
    assert(TargetPick.pick(Seq(row(1, 5.0, legal = false)), rankByScore = true).isEmpty)
  }

  test("the incumbent survives mill noise, a clear challenger takes it") {
    val rows = Seq(row(1, 10.0, current = true), row(2, 11.5))
    assert(TargetPick.pick(rows, rankByScore = true).contains(1))
    val dethroned = Seq(row(1, 10.0, current = true), row(2, 12.01))
    assert(TargetPick.pick(dethroned, rankByScore = true).contains(2))
  }

  test("an illegal incumbent never pins the pick") {
    val rows = Seq(row(1, 50.0, current = true, legal = false), row(2, 10.0))
    assert(TargetPick.pick(rows, rankByScore = true).contains(2))
  }

  test("a lone legal incumbent stands unopposed") {
    assert(TargetPick.pick(Seq(row(7, 3.0, current = true)), rankByScore = true).contains(7))
  }
}
