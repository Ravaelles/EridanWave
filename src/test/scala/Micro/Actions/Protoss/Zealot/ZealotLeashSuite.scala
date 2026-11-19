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

  test("the release sits a full tile inside the leash") {
    assert(ZealotLeash.leashReleaseTiles == 4.5)
    assert(ZealotLeash.leashReleaseTiles < ZealotLeash.leashTiles)
  }

  test("blades ball up past a tile with a melee swarm on them") {
    assert(ZealotLeash.ballTiles == 1.0)
    assert(ZealotLeash.shouldBall(mateTiles = 1.1, meleeFoes = 2))
    assert(ZealotLeash.shouldBall(mateTiles = 5.0, meleeFoes = 6))
    assert( ! ZealotLeash.shouldBall(mateTiles = 1.0, meleeFoes = 2))
    assert( ! ZealotLeash.shouldBall(mateTiles = 0.5, meleeFoes = 6))
    assert( ! ZealotLeash.shouldBall(mateTiles = 5.0, meleeFoes = 1))
    assert( ! ZealotLeash.shouldBall(mateTiles = 5.0, meleeFoes = 0))
  }
}
