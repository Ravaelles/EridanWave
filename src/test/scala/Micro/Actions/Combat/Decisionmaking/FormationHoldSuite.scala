package Micro.Actions.Combat.Decisionmaking

import org.scalatest.funsuite.AnyFunSuite

class FormationHoldSuite extends AnyFunSuite {

  // Range 6 tiles (192px), enemy edge distance 8 tiles (256px): 64px beyond range.
  private val range6Tiles   = 6 * 32.0
  private val enemy8Tiles   = 8 * 32.0
  private val beyondBy2Tiles = enemy8Tiles - range6Tiles

  test("holds the line when the enemy sits two tiles beyond our range") {
    assert(FormationHold.shouldHoldLine(
      pixelsBeyondRange = beyondBy2Tiles,
      melee             = false,
      hasFormation      = true,
      flanking          = false))
  }

  test("releases the line once the enemy is inside our range") {
    assert( ! FormationHold.shouldHoldLine(
      pixelsBeyondRange = -10.0,
      melee             = false,
      hasFormation      = true,
      flanking          = false))
  }

  test("releases the line exactly at max range") {
    assert( ! FormationHold.shouldHoldLine(
      pixelsBeyondRange = 0.0,
      melee             = false,
      hasFormation      = true,
      flanking          = false))
  }

  test("melee still closes even when outranged") {
    assert( ! FormationHold.shouldHoldLine(
      pixelsBeyondRange = beyondBy2Tiles,
      melee             = true,
      hasFormation      = true,
      flanking          = false))
  }

  test("nothing to hold without a formation") {
    assert( ! FormationHold.shouldHoldLine(
      pixelsBeyondRange = beyondBy2Tiles,
      melee             = false,
      hasFormation      = false,
      flanking          = false))
  }

  test("flankers keep circling instead of holding") {
    assert( ! FormationHold.shouldHoldLine(
      pixelsBeyondRange = beyondBy2Tiles,
      melee             = false,
      hasFormation      = true,
      flanking          = true))
  }

  test("advances only once every unit sits in its slot") {
    assert(FormationHold.shouldAdvanceInFormation(1.0))
    assert( ! FormationHold.shouldAdvanceInFormation(0.99))
    assert( ! FormationHold.shouldAdvanceInFormation(0.0))
  }

  test("fraction in slot counts units inside one tile of their slot") {
    assert(FormationHold.fractionInSlot(Seq(0.0, 10.0, 31.0)) == 1.0)
    assert(FormationHold.fractionInSlot(Seq(0.0, 10.0, 33.0)) == 2.0 / 3.0)
    assert(FormationHold.fractionInSlot(Seq.empty) == 1.0)
  }
}
