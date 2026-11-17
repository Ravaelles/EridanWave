package Tactic

import org.scalatest.funsuite.AnyFunSuite

class ForceGlobalAttackSuite extends AnyFunSuite {

  private def force(
    protoss: Boolean = true,
    terran: Boolean = false,
    templar: Boolean = true,
    detection: Boolean = false,
    macroGame: Boolean = true): Boolean =
    ForceGlobalAttack.shouldForce(protoss, terran, templar, detection, macroGame)

  test("the unseen blade forces the attack in Protoss and Terran macro games") {
    assert(force())
    assert(force(protoss = false, terran = true))
  }

  test("no blade, no force") {
    assert( ! force(templar = false))
  }

  test("detection ends the window") {
    assert( ! force(detection = true))
  }

  test("other matchups and baseless maps keep their own rules") {
    assert( ! force(protoss = false, terran = false))
    assert( ! force(macroGame = false))
  }

  test("covered statics wait for advantage, free shots stay legal") {
    assert(ForceGlobalAttack.mayHitStatic(forceActive = false, targetIsStaticDefense = true, advantaged = false, inOurRange = false))
    assert(ForceGlobalAttack.mayHitStatic(forceActive = true, targetIsStaticDefense = false, advantaged = false, inOurRange = false))
    assert( ! ForceGlobalAttack.mayHitStatic(forceActive = true, targetIsStaticDefense = true, advantaged = false, inOurRange = false))
    assert(ForceGlobalAttack.mayHitStatic(forceActive = true, targetIsStaticDefense = true, advantaged = true, inOurRange = false))
    assert(ForceGlobalAttack.mayHitStatic(forceActive = true, targetIsStaticDefense = true, advantaged = false, inOurRange = true))
  }
}
