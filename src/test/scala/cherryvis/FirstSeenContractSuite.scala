package cherryvis

import org.scalatest.funsuite.AnyFunSuite

import scala.collection.JavaConverters._

/**
 * Pins the vendored FirstSeen contract our deferred records rely on:
 * seven-frame delay, consume-on-poll, exactly-once records, frames
 * starting at one. If the library ever changes under us, this screams
 * before the viewer shows "???" again.
 */
class FirstSeenContractSuite extends AnyFunSuite {

  test("deferred ids come due seven frames later, consumed on poll") {
    val registry = new FirstSeen
    registry.defer(9, 100)
    assert(registry.pollDue(106).asScala.isEmpty)
    assert(registry.pollDue(107).asScala == Seq(9))
    assert(registry.pollDue(200).asScala.isEmpty)
  }

  test("records are exactly once and frames start at one") {
    val registry = new FirstSeen
    registry.record(0, 9, 65, 1, 2)
    registry.record(50, 9, 65, 3, 4)
    assert(registry.toJson == """"1":[{"id":9,"type":65,"x":1,"y":2}]""")
  }
}
