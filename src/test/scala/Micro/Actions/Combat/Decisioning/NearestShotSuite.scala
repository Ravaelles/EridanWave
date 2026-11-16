package Micro.Actions.Combat.Decisioning

import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.UnitInfo
import bwapi.UnitType
import org.scalatest.funsuite.AnyFunSuite
import testkit.TestKit

class NearestShotSuite extends AnyFunSuite {

  private def dragoon(id: Int, x: Int): UnitInfo =
    TestKit.friendly(UnitType.Protoss_Dragoon, id, x, 0)

  test("empty candidates pick nothing") {
    TestKit.seedWorld()
    assert(NearestShot.pick(Seq.empty, _.id) == None)
  }

  test("lowest rank wins") {
    TestKit.seedWorld()
    val first = dragoon(1, 32)
    val second = dragoon(2, 320)
    assert(NearestShot.pick(Seq(second, first), _.id).contains(first))
  }

  test("a custom rank overrides insertion order") {
    TestKit.seedWorld()
    val weak = dragoon(1, 32)
    val strong = dragoon(2, 320)
    assert(NearestShot.pick(Seq(weak, strong), -_.id).contains(strong))
    assert(NearestShot.pick(Seq(weak, strong), _.id).contains(weak))
  }
}
