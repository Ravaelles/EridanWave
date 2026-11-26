package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

/**
 * Melee shared chew (rule 19: squad assignment as a Target pick, not a
 * second ladder): pack blades chew one target instead of each dueling
 * its own. Ranged focus needs volleys to line up; blades need contact
 * to line up - the weakest enemy two pack mates can both reach dies
 * first, then the pack re-picks together. Pure pick for the suite; the
 * ladder calls the thin seam.
 */
class MeleeFocusSuite extends AnyFunSuite {

  test("swings to kill rounds up and never returns zero") {
    assert(MeleeFocus.swingsToKill(healthTotal = 32.0, packDamage = 16.0) == 2)
    assert(MeleeFocus.swingsToKill(healthTotal = 33.0, packDamage = 16.0) == 3)
    assert(MeleeFocus.swingsToKill(healthTotal = 5.0, packDamage = 16.0) == 1)
  }

  test("no pack damage cannot kill anything") {
    assert(MeleeFocus.swingsToKill(healthTotal = 32.0, packDamage = 0.0) == Int.MaxValue)
  }
}
