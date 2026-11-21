package Micro.Actions.Combat.Maneuvering

import org.scalatest.funsuite.AnyFunSuite

class RetreatSuite extends AnyFunSuite {

  test("flee destinations inside two tiles push past instead of braking") {
    assert(Retreat.pushPastArrival(distTiles = 0.5, preciseArrival = false))
    assert(Retreat.pushPastArrival(distTiles = 1.9, preciseArrival = false))
    assert( ! Retreat.pushPastArrival(distTiles = 2.0, preciseArrival = false))
    assert( ! Retreat.pushPastArrival(distTiles = 5.0, preciseArrival = false))
  }

  test("precision maneuvers always arrive exactly") {
    assert( ! Retreat.pushPastArrival(distTiles = 0.5, preciseArrival = true))
    assert( ! Retreat.pushPastArrival(distTiles = 0.0, preciseArrival = true))
  }

  test("the push band and length ride named constants") {
    assert(Retreat.arrivalPushTiles == 2.0)
    assert(Retreat.arrivalPushPastTiles == 4.0)
  }
}
