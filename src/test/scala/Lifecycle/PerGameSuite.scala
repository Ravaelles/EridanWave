package Lifecycle

import org.scalatest.funsuite.AnyFunSuite

class PerGameSuite extends AnyFunSuite {

  test("registered subsystems all reset together") {
    var first = 0
    var second = 0
    new PerGame { def reset(): Unit = { first += 1 } }
    new PerGame { def reset(): Unit = { second += 1 } }
    PerGame.resetAll()
    assert(first == 1 && second == 1)
  }
}
