package Gameplans.Protoss.PvT

import org.scalatest.funsuite.AnyFunSuite

class ForwardGatewaysSuite extends AnyFunSuite {

  test("a bank over eight hundred means thirteen gates at least") {
    assert(ForwardGateways.shouldBankGates(801))
    assert(ForwardGateways.shouldBankGates(5000))
    assert( ! ForwardGateways.shouldBankGates(800))
    assert(ForwardGateways.minGateways == 13)
  }

  test("forward building starts at three bases") {
    assert(ForwardGateways.shouldBuildForward(3))
    assert(ForwardGateways.shouldBuildForward(5))
    assert( ! ForwardGateways.shouldBuildForward(2))
  }

  test("forward means closer to the enemy than our natural choke") {
    assert(ForwardGateways.isForward(distToEnemyTiles = 99.9, chokeToEnemyTiles = 100.0))
    assert( ! ForwardGateways.isForward(distToEnemyTiles = 100.0, chokeToEnemyTiles = 100.0))
    assert( ! ForwardGateways.isForward(distToEnemyTiles = 200.0, chokeToEnemyTiles = 100.0))
  }
}
