package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class FerryFetchSuite extends AnyFunSuite {

  private def fetch(
    main: Double = 0.0,
    still: Boolean = true,
    reaver: Double = 0.0,
    foe: Double = 100.0): Boolean =
    FerryFetch.shouldFetch(main, still, reaver, foe)

  test("an idle home hull fetches a close Reaver under clear skies") {
    assert(fetch())
    assert(fetch(main = 30.0, reaver = 20.0, foe = 11.0))
  }

  test("a moving hull never diverts") {
    assert( ! fetch(still = false))
  }

  test("a far hull or a far Reaver stays out") {
    assert( ! fetch(main = 30.01))
    assert( ! fetch(reaver = 20.01))
  }

  test("a ground enemy inside eleven tiles vetoes the pickup") {
    assert( ! fetch(foe = 10.99))
  }
}
