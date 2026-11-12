package Debugging

import org.scalatest.funsuite.AnyFunSuite

class FormationAuditStatsSuite extends AnyFunSuite {

  test("no breaks means no stats") {
    assert(FormationAuditStats.summarize(Seq.empty).isEmpty)
  }

  test("a single break has zero spread") {
    val stats = FormationAuditStats.summarize(Seq(3.0)).get
    assert(stats.n == 1)
    assert(stats.mean == 3.0)
    assert(stats.stddev == 0.0)
    assert(stats.min == 3.0)
    assert(stats.max == 3.0)
  }

  test("mean and population stddev over several breaks") {
    val stats = FormationAuditStats.summarize(Seq(1.0, 2.0, 3.0, 4.0)).get
    assert(stats.n == 4)
    assert(stats.mean == 2.5)
    assert(math.abs(stats.stddev - math.sqrt(1.25)) < 1e-9)
    assert(stats.min == 1.0)
    assert(stats.max == 4.0)
  }

  test("breaks group per own class") {
    import FormationAudit.BreakRow
    val rows = Seq(
      BreakRow(10, 1, "Dragoon", "Dragoon", 2.0),
      BreakRow(12, 2, "Dragoon", "Dragoon", 4.0),
      BreakRow(14, 3, "Zealot", "Zergling", 0.5))
    val byClass = FormationAuditStats.byClass(rows)
    assert(byClass.keySet == Set("Dragoon", "Zealot"))
    assert(byClass("Dragoon").n == 2)
    assert(byClass("Dragoon").mean == 3.0)
    assert(byClass("Zealot").min == 0.5)
  }

  test("the rising edge logs once per break, not every frame") {
    FormationAudit.reset()
    FormationAudit.noteBreakState(10, 1, "Dragoon", broken = false, Some(5.0), Some("Dragoon"))
    FormationAudit.noteBreakState(11, 1, "Dragoon", broken = true, Some(4.0), Some("Dragoon"))
    FormationAudit.noteBreakState(12, 1, "Dragoon", broken = true, Some(3.0), Some("Dragoon"))
    FormationAudit.noteBreakState(13, 1, "Dragoon", broken = false, Some(3.0), Some("Dragoon"))
    FormationAudit.noteBreakState(14, 1, "Dragoon", broken = true, Some(2.0), Some("Dragoon"))
    val rows = FormationAudit.breakRows
    assert(rows.map(_.frame) == Seq(11, 14))
    assert(rows.map(_.distTiles) == Seq(4.0, 2.0))
    FormationAudit.reset()
    assert(FormationAudit.breakRows.isEmpty)
  }

  test("breaks without a live target never reach the stats") {
    FormationAudit.reset()
    FormationAudit.noteBreakState(10, 1, "Dragoon", broken = true, None, None)
    assert(FormationAudit.breakRows.isEmpty)
    FormationAudit.reset()
  }

  test("dress verdicts accumulate per unit-frame and clear on reset") {
    FormationAudit.reset()
    FormationAudit.noteDress(10, 1, "Dragoon", 3.0, 2.5, 0.5, 14, "Cautious", "StepBack")
    FormationAudit.noteDress(11, 1, "Dragoon", 2.8, 2.5, 0.5, 14, "Cautious", "HoldLine")
    val rows = FormationAudit.dressRows
    assert(rows.size == 2)
    assert(rows.map(_.order) == Seq("StepBack", "HoldLine"))
    assert(rows.head.lineMeanTiles == 2.5)
    FormationAudit.reset()
    assert(FormationAudit.dressRows.isEmpty)
  }

  test("the tally counts every leaf label") {
    FormationAudit.reset()
    FormationAudit.tally("Engage/Approach")
    FormationAudit.tally("Engage/Approach")
    FormationAudit.tally("Reposition")
    assert(FormationAudit.tallySnapshot == Map("Engage/Approach" -> 2, "Reposition" -> 1))
    FormationAudit.reset()
  }
}
