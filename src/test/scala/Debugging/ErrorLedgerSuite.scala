package Debugging

import org.scalatest.funsuite.AnyFunSuite

class ErrorLedgerSuite extends AnyFunSuite {

  test("repeats of one key collapse into a count with first and last frames") {
    ErrorLedger.reset()
    Lifecycle.With.frame = 10
    ErrorLedger.note("k1", ErrorLedger.Priority.High, "boom")
    Lifecycle.With.frame = 20
    ErrorLedger.note("k1", ErrorLedger.Priority.High, "boom again")
    Lifecycle.With.frame = 30
    ErrorLedger.note("k1", ErrorLedger.Priority.High, "boom a third time")
    val notes = ErrorLedger.notes
    assert(notes.size == 1)
    assert(notes.head.count == 3)
    assert(notes.head.firstFrame == 10)
    assert(notes.head.lastFrame == 30)
    ErrorLedger.reset()
  }

  test("only a few samples are kept per key") {
    ErrorLedger.reset()
    (1 to 10).foreach(i => ErrorLedger.note("k", ErrorLedger.Priority.Low, "m" + i))
    val note = ErrorLedger.notes.head
    assert(note.samples == Vector("m1", "m2", "m3"))
    ErrorLedger.reset()
  }

  test("priority filter orders worst first") {
    ErrorLedger.reset()
    (1 to 5).foreach(_ => ErrorLedger.note("low-many", ErrorLedger.Priority.Low, "x"))
    ErrorLedger.note("high-once", ErrorLedger.Priority.High, "x")
    ErrorLedger.note("medium-once", ErrorLedger.Priority.Medium, "x")
    val filtered = ErrorLedger.atLeast(ErrorLedger.Priority.Medium).map(_.key)
    assert(filtered == Seq("high-once", "medium-once"))
    ErrorLedger.reset()
  }

  test("empty ledger renders an empty array") {
    ErrorLedger.reset()
    assert(ErrorLedger.toJson == "[]")
  }

  test("json escapes quotes, backslashes and newlines") {
    ErrorLedger.reset()
    ErrorLedger.note("k\"ey", ErrorLedger.Priority.High, "a\\b\nc\"d")
    val json = ErrorLedger.toJson
    assert(json.contains("\"key\":\"k\\\"ey\""))
    assert(json.contains("\"samples\":[\"a\\\\b\\nc\\\"d\"]"))
    ErrorLedger.reset()
  }

  test("note returns the running count for trace throttling") {
    ErrorLedger.reset()
    assert(ErrorLedger.note("k", ErrorLedger.Priority.High, "a") == 1)
    assert(ErrorLedger.note("k", ErrorLedger.Priority.High, "b") == 2)
    assert(ErrorLedger.note("k", ErrorLedger.Priority.High, "c") == 3)
    ErrorLedger.reset()
  }

  test("full traces are for the first hits only") {
    assert(ErrorLedger.deservesFullTrace(1))
    assert(ErrorLedger.deservesFullTrace(2))
    assert( ! ErrorLedger.deservesFullTrace(3))
  }

  test("exception keys name the class and the top in-repo frame") {
    val exception = new RuntimeException("bang")
    exception.setStackTrace(Array(
      new StackTraceElement("java.lang.Thread", "getStackTrace", "Thread.java", 100),
      new StackTraceElement("Tactic.Squads.SquadAttack", "run", "SquadAttack.scala", 42),
      new StackTraceElement("Lifecycle.EridanWave", "onFrame", "EridanWave.scala", 7)))
    assert(ErrorLedger.exceptionKey(exception) == "exception.RuntimeException.at.SquadAttack:42")
  }

  test("exceptions outside our packages key on unknown") {
    val exception = new RuntimeException("bang")
    exception.setStackTrace(Array(
      new StackTraceElement("java.lang.Thread", "getStackTrace", "Thread.java", 100)))
    assert(ErrorLedger.exceptionKey(exception) == "exception.RuntimeException.at.unknown")
  }
}
