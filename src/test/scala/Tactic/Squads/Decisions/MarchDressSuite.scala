package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class MarchDressSuite extends AnyFunSuite {

  test("no enemy inside twenty tiles means no dressing") {
    assert( ! MarchDress.enemyNear(20.01))
    assert( ! MarchDress.enemyNear(200.0))
    assert(MarchDress.enemyNear(20.0))
    assert(MarchDress.enemyNear(0.0))
  }

  test("an armed enemy in sight always dresses") {
    assert( ! MarchDress.shouldSkipWait(1000.0, 0.0, armedEnemyVisible = true))
  }

  test("a weak or strung-out pack dresses even unopposed") {
    assert( ! MarchDress.shouldSkipWait(149.0, 1.0, armedEnemyVisible = false))
    assert( ! MarchDress.shouldSkipWait(500.0, 3.01, armedEnemyVisible = false))
  }

  test("a strong compact pack marches through instead of dressing") {
    assert(MarchDress.shouldSkipWait(150.0, 3.0, armedEnemyVisible = false))
    assert(MarchDress.shouldSkipWait(900.0, 0.5, armedEnemyVisible = false))
  }

  test("six friendlies inside four tiles never wait for the line") {
    assert(MarchDress.isMassed(6))
    assert(MarchDress.isMassed(40))
    assert( ! MarchDress.isMassed(5))
    assert( ! MarchDress.isMassed(0))
  }

  test("nobody dresses inside static reach") {
    assert(MarchDress.staticNear(0.0))
    assert(MarchDress.staticNear(8.0))
    assert( ! MarchDress.staticNear(8.01))
  }

  test("nobody rallies or slots inside ten tiles of static guns") {
    assert(MarchDress.shouldPushPastStatics(0.0))
    assert(MarchDress.shouldPushPastStatics(10.0))
    assert( ! MarchDress.shouldPushPastStatics(10.01))
  }

  test("static push latches: enter at ten, hold to twelve, exit past it") {
    val id = 777001
    assert( ! MarchDress.pushPastStatics(id, 13.0))
    assert(MarchDress.pushPastStatics(id, 9.0))
    assert(MarchDress.pushPastStatics(id, 11.0))
    assert(MarchDress.pushPastStatics(id, 12.0))
    assert( ! MarchDress.pushPastStatics(id, 12.01))
    assert( ! MarchDress.pushPastStatics(id, 11.0))
    assert( ! MarchDress.pushPastStatics(777002, 10.5))
  }

  test("dress wait latches: enter past two and a half, march below one and a half") {
    val id = 424242
    assert( ! MarchDress.holdDress(id, 0.0))
    assert(MarchDress.holdDress(id, 3.0))
    assert(MarchDress.holdDress(id, 2.0))
    assert(MarchDress.holdDress(id, 2.0))
    assert( ! MarchDress.holdDress(id, 1.0))
    assert( ! MarchDress.holdDress(id, 2.0))
    assert(MarchDress.holdDress(424243, 2.2))
    assert( ! MarchDress.holdDress(424244, 1.8))
  }

  test("a hull with a shot holds nothing") {
    assert(MarchDress.shouldHoldForPack(true, true, false, false, false, holdDress = true, targetsInRange = false))
    assert( ! MarchDress.shouldHoldForPack(true, true, false, false, false, holdDress = true, targetsInRange = true))
    assert( ! MarchDress.shouldHoldForPack(false, true, false, false, false, holdDress = true, targetsInRange = false))
    assert( ! MarchDress.shouldHoldForPack(true, false, false, false, false, holdDress = true, targetsInRange = false))
    assert( ! MarchDress.shouldHoldForPack(true, true, true, false, false, holdDress = true, targetsInRange = false))
    assert( ! MarchDress.shouldHoldForPack(true, true, false, true, false, holdDress = true, targetsInRange = false))
    assert( ! MarchDress.shouldHoldForPack(true, true, false, false, true, holdDress = true, targetsInRange = false))
    assert( ! MarchDress.shouldHoldForPack(true, true, false, false, false, holdDress = false, targetsInRange = false))
  }
}
