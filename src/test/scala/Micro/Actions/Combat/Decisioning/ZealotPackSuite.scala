package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class ZealotPackSuite extends AnyFunSuite {

  private def regroup(
    onlyZealots: Boolean = true,
    zealots: Int = 4,
    mateTiles: Double = Double.PositiveInfinity,
    usSkim: Double = 0.0,
    foeSkim: Double = 0.0,
    inContact: Boolean = false,
    enemyClosing: Boolean = false): Boolean =
    ZealotPack.shouldRegroup(onlyZealots, zealots, mateTiles, usSkim, foeSkim, inContact, enemyClosing)

  test("a lone hull of a small zealot ball returns to the centroid") {
    assert(regroup())
    assert(regroup(zealots = 11))
  }

  test("a full ball and mixed armies approach freely") {
    assert( ! regroup(zealots = 12))
    assert( ! regroup(onlyZealots = false))
  }

  test("a packmate inside one tile releases the approach") {
    assert( ! regroup(mateTiles = 1.0))
    assert( ! regroup(mateTiles = 0.5))
    assert(regroup(mateTiles = 1.01))
  }

  test("an engaged zealot holds the line instead of regrouping") {
    assert( ! regroup(inContact = true))
    assert( ! regroup(mateTiles = 5.0, inContact = true))
    assert(regroup(mateTiles = 5.0, inContact = false))
  }

  test("a closing enemy is contact: mutual approach meets, never regroups") {
    assert( ! regroup(mateTiles = 5.0, enemyClosing = true))
    assert(regroup(mateTiles = 5.0, enemyClosing = false))
  }

  test("a two-to-one local edge fights instead of regrouping") {
    assert( ! regroup(usSkim = 200.0, foeSkim = 100.0))
    assert(regroup(usSkim = 199.0, foeSkim = 100.0))
    assert(regroup(usSkim = 200.0, foeSkim = 0.0))
  }
}
