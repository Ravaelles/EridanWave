package Micro.Actions.Combat.Decisionmaking

import org.scalatest.funsuite.AnyFunSuite

class FormationHoldSuite extends AnyFunSuite {

  // All distances in tiles (1 tile = 32 px, as in Atlantis).

  test("holds the line when the enemy sits two tiles beyond our range") {
    assert(FormationHold.shouldHoldLine(
      tilesBeyondRange  = 2.0,
      melee             = false,
      hasFormation      = true,
      flanking          = false,
      allyEngaged       = false,
      enemyFired        = false))
  }

  test("starts the fight once the enemy is one tile beyond our range") {
    assert( ! FormationHold.shouldHoldLine(
      tilesBeyondRange  = 1.0,
      melee             = false,
      hasFormation      = true,
      flanking          = false,
      allyEngaged       = false,
      enemyFired        = false))
  }

  test("releases the line exactly at the 1.1 tile band edge") {
    assert( ! FormationHold.shouldHoldLine(
      tilesBeyondRange  = FormationHold.holdReleaseTiles,
      melee             = false,
      hasFormation      = true,
      flanking          = false,
      allyEngaged       = false,
      enemyFired        = false))
  }

  test("holds just outside the release band") {
    assert(FormationHold.shouldHoldLine(
      tilesBeyondRange  = FormationHold.holdReleaseTiles + 0.1,
      melee             = false,
      hasFormation      = true,
      flanking          = false,
      allyEngaged       = false,
      enemyFired        = false))
  }

  test("releases the line once the enemy is inside our range") {
    assert( ! FormationHold.shouldHoldLine(
      tilesBeyondRange  = -0.5,
      melee             = false,
      hasFormation      = true,
      flanking          = false,
      allyEngaged       = false,
      enemyFired        = false))
  }

  test("melee still closes even when far outranged") {
    assert( ! FormationHold.shouldHoldLine(
      tilesBeyondRange  = 3.0,
      melee             = true,
      hasFormation      = true,
      flanking          = false,
      allyEngaged       = false,
      enemyFired        = false))
  }

  test("nothing to hold without a formation") {
    assert( ! FormationHold.shouldHoldLine(
      tilesBeyondRange  = 3.0,
      melee             = false,
      hasFormation      = false,
      flanking          = false,
      allyEngaged       = false,
      enemyFired        = false))
  }

  test("flankers keep circling instead of holding") {
    assert( ! FormationHold.shouldHoldLine(
      tilesBeyondRange  = 3.0,
      melee             = false,
      hasFormation      = true,
      flanking          = true,
      allyEngaged       = false,
      enemyFired        = false))
  }

  test("joins the fight once a cluster mate engages") {
    assert( ! FormationHold.shouldHoldLine(
      tilesBeyondRange  = 3.0,
      melee             = false,
      hasFormation      = true,
      flanking          = false,
      allyEngaged       = true,
      enemyFired        = false))
  }

  test("joins the fight once an enemy in range opened fire") {
    assert( ! FormationHold.shouldHoldLine(
      tilesBeyondRange  = 3.0,
      melee             = false,
      hasFormation      = true,
      flanking          = false,
      allyEngaged       = false,
      enemyFired        = true))
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
