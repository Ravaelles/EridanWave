package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class ReaverShuttlePairSuite extends AnyFunSuite {

  test("same squad matches, different squads do not") {
    val squad = new Object
    assert(ReaverShuttlePair.sameSquadOrUnstaffed(Some(squad), Some(squad), lenient = false))
    assert( ! ReaverShuttlePair.sameSquadOrUnstaffed(Some(new Object), Some(new Object), lenient = false))
  }

  test("strict mode keeps a squadded ride blind to squadless hulls") {
    assert(ReaverShuttlePair.sameSquadOrUnstaffed(None, Some(new Object), lenient = false))
    assert( ! ReaverShuttlePair.sameSquadOrUnstaffed(Some(new Object), None, lenient = false))
    assert(ReaverShuttlePair.sameSquadOrUnstaffed(None, None, lenient = false))
  }

  test("lenient mode bonds blind on baseless maps: staffing dice never strand the pair") {
    assert(ReaverShuttlePair.sameSquadOrUnstaffed(Some(new Object), Some(new Object), lenient = true))
    assert(ReaverShuttlePair.sameSquadOrUnstaffed(None, Some(new Object), lenient = true))
    assert(ReaverShuttlePair.sameSquadOrUnstaffed(Some(new Object), None, lenient = true))
    assert(ReaverShuttlePair.sameSquadOrUnstaffed(None, None, lenient = true))
  }
}
