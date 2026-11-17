package Macro.Actions

import org.scalatest.funsuite.AnyFunSuite

class ShouldCancelBaseSuite extends AnyFunSuite {

  test("a shown DT plus a morphing base plus no Forge money cancels the base") {
    assert(ShouldCancelBase.shouldCancel(
      enemyDarkTemplarShown = true,
      baseInProduction = true,
      forgeAffordable = false))
  }

  test("no shown DT never cancels") {
    assert( ! ShouldCancelBase.shouldCancel(
      enemyDarkTemplarShown = false,
      baseInProduction = true,
      forgeAffordable = false))
  }

  test("no morphing base never cancels") {
    assert( ! ShouldCancelBase.shouldCancel(
      enemyDarkTemplarShown = true,
      baseInProduction = false,
      forgeAffordable = false))
  }

  test("an affordable Forge never cancels the base") {
    assert( ! ShouldCancelBase.shouldCancel(
      enemyDarkTemplarShown = true,
      baseInProduction = true,
      forgeAffordable = true))
  }
}
