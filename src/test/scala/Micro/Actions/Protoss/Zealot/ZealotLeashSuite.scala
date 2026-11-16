package Micro.Actions.Protoss.Zealot

import org.scalatest.funsuite.AnyFunSuite

class ZealotLeashSuite extends AnyFunSuite {

  test("the leash holds at five and a half tiles") {
    assert(ZealotLeash.leashTiles == 5.5)
    assert( ! ZealotLeash.shouldRejoin(0.0))
    assert( ! ZealotLeash.shouldRejoin(5.5))
    assert(ZealotLeash.shouldRejoin(5.6))
    assert(ZealotLeash.shouldRejoin(20.0))
  }
}
