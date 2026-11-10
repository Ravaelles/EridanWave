package Micro.Actions.Combat.Decisioning.Kiting

import org.scalatest.funsuite.AnyFunSuite

class FacingAwaySuite extends AnyFunSuite {

  test("looking straight at us faces us") {
    assert(FacingAway.facingUs(foeAngle = 0.0, radiansFoeToUs = 0.0))
    assert( ! FacingAway.showingBack(foeAngle = 0.0, radiansFoeToUs = 0.0))
  }

  test("angle wraps through pi") {
    assert(FacingAway.facingUs(foeAngle = 3.0, radiansFoeToUs = -3.0))
    assert( ! FacingAway.showingBack(foeAngle = 3.0, radiansFoeToUs = -3.0))
  }

  test("opposite direction shows the back") {
    assert(FacingAway.showingBack(foeAngle = 0.0, radiansFoeToUs = Math.PI))
    assert( ! FacingAway.facingUs(foeAngle = 0.0, radiansFoeToUs = Math.PI))
  }

  test("sideways is neither facing nor back") {
    assert( ! FacingAway.facingUs(foeAngle = 0.0, radiansFoeToUs = Math.PI / 2))
    assert( ! FacingAway.showingBack(foeAngle = 0.0, radiansFoeToUs = Math.PI / 2))
  }

  test("only turned-away melee is dismissed") {
    assert(FacingAway.dismissMelee(isMelee = true, foeShowsBack = true))
    assert( ! FacingAway.dismissMelee(isMelee = true, foeShowsBack = false))
    assert( ! FacingAway.dismissMelee(isMelee = false, foeShowsBack = true))
    assert( ! FacingAway.dismissMelee(isMelee = false, foeShowsBack = false))
  }
}
