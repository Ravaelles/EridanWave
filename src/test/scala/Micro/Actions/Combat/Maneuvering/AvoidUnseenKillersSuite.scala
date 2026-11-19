package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class AvoidUnseenKillersSuite extends AnyFunSuite {

  test("the dodge starts inside the radius, never outside") {
    assert(AvoidUnseenKillers.shouldAvoid(7.99, 8.0))
    assert(AvoidUnseenKillers.shouldAvoid(0.0, 8.0))
    assert( ! AvoidUnseenKillers.shouldAvoid(8.0, 8.0))
    assert( ! AvoidUnseenKillers.shouldAvoid(20.0, 8.0))
  }

  test("the escape point sits straight away at the stride") {
    assert(AvoidUnseenKillers.awayFrom(Pixel(200, 200), Pixel(200, 100), 10.0) == Pixel(200, 200 + 320))
    assert(AvoidUnseenKillers.awayFrom(Pixel(200, 200), Pixel(100, 200), 10.0) == Pixel(200 + 320, 200))
  }

  test("lurkers keep a spine-length berth, templars a blade-length one") {
    assert(AvoidLurkers.avoidTiles == 8.0)
    assert(AvoidDarkTemplar.avoidTiles == 4.0)
    assert(AvoidDarkTemplar.workerAvoidTiles == 5.5)
    assert(AvoidDarkTemplar.avoidTilesFor(isWorker = true) == 5.5)
    assert(AvoidDarkTemplar.avoidTilesFor(isWorker = false) == 4.0)
  }
}
