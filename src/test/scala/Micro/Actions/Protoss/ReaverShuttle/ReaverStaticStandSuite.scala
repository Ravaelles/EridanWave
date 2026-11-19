package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class ReaverStaticStandSuite extends AnyFunSuite {

  test("a fresh loaded hull with an old shot holds its line") {
    assert(ReaverStaticStand.mayStand(shieldWoundHp = 0, scarabs = 5, framesSinceShot = 1000))
    assert(ReaverStaticStand.mayStand(shieldWoundHp = 40, scarabs = 1, framesSinceShot = 144))
  }

  test("a holed hull yields before firing") {
    assert( ! ReaverStaticStand.mayStand(shieldWoundHp = 41, scarabs = 5, framesSinceShot = 1000))
    assert( ! ReaverStaticStand.mayStand(shieldWoundHp = 80, scarabs = 5, framesSinceShot = 1000))
  }

  test("a dry hull holds nothing") {
    assert( ! ReaverStaticStand.mayStand(shieldWoundHp = 0, scarabs = 0, framesSinceShot = 1000))
  }

  test("a just-fired hull withdraws on the cooldown") {
    assert( ! ReaverStaticStand.mayStand(shieldWoundHp = 0, scarabs = 5, framesSinceShot = 143))
    assert(ReaverStaticStand.mayStand(shieldWoundHp = 0, scarabs = 5, framesSinceShot = 144))
  }
}
