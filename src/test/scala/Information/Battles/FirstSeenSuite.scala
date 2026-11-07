package Information.Battles

import cherryvis.{FirstSeen, Json, StringTable}
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.JavaConverters._

class FirstSeenSuite extends AnyFunSuite {

  private def due(seen: FirstSeen, now: Int): Seq[Int] =
    seen.pollDue(now).asScala.map(_.intValue()).toSeq

  test("deferred units surface once their position settles") {
    val seen = new FirstSeen
    seen.defer(7, 100)
    assert(due(seen, 100 + FirstSeen.DEFER_FRAMES - 1).isEmpty)
    assert(due(seen, 100 + FirstSeen.DEFER_FRAMES) == Seq(7))
    assert(due(seen, 100 + FirstSeen.DEFER_FRAMES).isEmpty, "polled ids are consumed")
  }

  test("first record wins, later duplicates are dropped") {
    val seen = new FirstSeen
    seen.record(10, 7, 65, 100, 200)
    seen.record(20, 7, 65, 300, 400)
    assert(seen.toJson == "\"10\":[{\"id\":7,\"type\":65,\"x\":100,\"y\":200}]", seen.toJson)
  }

  test("clear resets first-seen, engine ids and pending") {
    val seen = new FirstSeen
    seen.record(10, 7, 65, 1, 2)
    seen.noteEngineId(7, 70)
    seen.defer(8, 10)
    seen.clear()
    assert(seen.toJson.isEmpty)
    assert(seen.engineIdsJson.isEmpty)
    assert(due(seen, Int.MaxValue).isEmpty)
  }
}

class StringTableSuite extends AnyFunSuite {

  test("unknown strings get fresh ids in first-seen order") {
    val table = new StringTable
    assert(table.get("b") == 10000)
    assert(table.get("a") == 10001)
    assert(table.get("b") == 10000, "repeat lookups reuse the id")
    assert(!table.isEmpty)
    assert(new StringTable().isEmpty)
  }

  test("null maps to the missing sentinel") {
    assert(new StringTable().get(null) == -1)
  }

  test("json escaping neutralizes quotes and backslashes") {
    assert(Json.escape("say \"hi\" \\ ok") == "say \\\"hi\\\" \\\\ ok")
    assert(Json.escape(null) == "")
  }
}
