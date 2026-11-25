package Debugging

import org.scalatest.funsuite.AnyFunSuite

class FlapLedgerSuite extends AnyFunSuite {

  private def feed(id: Int, kinds: Seq[FlapLedger.OrderKind], startFrame: Int = 100, step: Int = 1): Unit =
    kinds.zipWithIndex.foreach { case (kind, i) =>
      FlapLedger.note(id, "Dragoon", kind, startFrame + i * step)
    }

  test("quick stand-vs-go alternation reports, steady orders stay quiet") {
    FlapLedger.reset()
    feed(7, Seq.fill(7)(Seq(FlapLedger.MoveKind, FlapLedger.HoldKind)).flatten)
    assert(FlapLedger.summary().contains("Dragoon #7 13x"))
    FlapLedger.reset()
    feed(7, Seq.fill(20)(FlapLedger.MoveKind))
    assert(FlapLedger.summary() == "### Order flap: none")
  }

  test("slow cycles and locomotion-only pairs never count") {
    FlapLedger.reset()
    feed(7, Seq(FlapLedger.MoveKind, FlapLedger.HoldKind, FlapLedger.MoveKind), step = 10)
    feed(8, Seq(FlapLedger.MoveKind, FlapLedger.AttackMoveKind, FlapLedger.MoveKind))
    feed(9, Seq(FlapLedger.AttackKind, FlapLedger.MoveKind, FlapLedger.AttackKind))
    assert(FlapLedger.summary() == "### Order flap: none")
  }

  test("below-threshold flaps stay quiet until they persist") {
    FlapLedger.reset()
    feed(7, Seq(FlapLedger.MoveKind, FlapLedger.HoldKind, FlapLedger.MoveKind))
    assert(FlapLedger.summary() == "### Order flap: none")
    feed(7, Seq.fill(5)(Seq(FlapLedger.HoldKind, FlapLedger.MoveKind)).flatten, startFrame = 103)
    assert(FlapLedger.summary().contains("Dragoon #7"))
  }

  test("reset clears the board for the next game") {
    FlapLedger.reset()
    feed(7, Seq.fill(7)(Seq(FlapLedger.MoveKind, FlapLedger.HoldKind)).flatten)
    FlapLedger.reset()
    assert(FlapLedger.summary() == "### Order flap: none")
  }
}
