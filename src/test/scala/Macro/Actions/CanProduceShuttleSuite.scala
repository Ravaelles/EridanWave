package Macro.Actions

import org.scalatest.funsuite.AnyFunSuite

class CanProduceShuttleSuite extends AnyFunSuite {

  private def produce(
    observers: Boolean = false,
    protoss: Boolean = false,
    dragoons: Int = 20,
    score: Double = 100.0): Boolean =
    CanProduceShuttle.canProduce(observers, protoss, dragoons, score)

  test("observers eat the Shuttle gas in any matchup") {
    assert( ! produce(observers = true))
    assert( ! produce(observers = true, protoss = true, dragoons = 20, score = 500.0))
    assert(produce(observers = false, protoss = false, dragoons = 0))
  }

  test("a short Dragoon line never queues a Shuttle vs Protoss") {
    assert( ! produce(protoss = true, dragoons = 0))
    assert( ! produce(protoss = true, dragoons = 6, score = 500.0))
    assert( ! produce(protoss = true, dragoons = 7, score = 100.0))
  }

  test("only a crushing lead buys the ferry below a dozen Dragoons") {
    assert( ! produce(protoss = true, dragoons = 11, score = 250.0))
    assert(produce(protoss = true, dragoons = 11, score = 250.1))
    assert(produce(protoss = true, dragoons = 7, score = 500.0))
  }

  test("a standing line builds the ferry without asking the score") {
    assert(produce(protoss = true, dragoons = 12, score = 0.0))
    assert(produce(protoss = true, dragoons = 20))
  }
}
