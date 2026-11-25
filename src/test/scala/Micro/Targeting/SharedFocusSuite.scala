package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class SharedFocusSuite extends AnyFunSuite {

  private def bid(id: Int, reach: Int, cost: Int): SharedFocus.Bid =
    SharedFocus.Bid(id, reach, cost)

  test("reach churn holds the committed pick") {
    // Live 4+3 pattern: three hulls on 165 while 182 flickers 2<->3 reach.
    val current = Some(bid(165, reach = 2, cost = 3))
    val churned = Seq(bid(165, reach = 2, cost = 3), bid(182, reach = 3, cost = 3))

    assert(SharedFocus.hold(current, churned).contains(bid(165, reach = 2, cost = 3)))
  }

  test("a dead or vanished target releases") {
    val current = Some(bid(165, reach = 2, cost = 3))

    assert(SharedFocus.hold(current, Seq(bid(182, reach = 3, cost = 3))).isEmpty)
    assert(SharedFocus.hold(current, Seq.empty).isEmpty)
  }

  test("zero reach releases back to the ladder") {
    val current = Some(bid(165, reach = 2, cost = 3))
    val stranded = Seq(bid(165, reach = 0, cost = 3), bid(182, reach = 2, cost = 2))

    assert(SharedFocus.hold(current, stranded).isEmpty)
  }

  test("a one-volley free kill preempts the grind") {
    // The incumbent needs three volleys; a challenger dies to one now.
    val current = Some(bid(165, reach = 2, cost = 3))
    val freeKill = Seq(bid(165, reach = 2, cost = 3), bid(182, reach = 2, cost = 1))

    assert(SharedFocus.hold(current, freeKill).contains(bid(182, reach = 2, cost = 1)))
  }

  test("no incumbent means no hold - the caller chooses fresh") {
    val bids = Seq(bid(165, reach = 2, cost = 3), bid(182, reach = 3, cost = 3))

    assert(SharedFocus.hold(None, bids).isEmpty)
  }

  test("a cheaper grind does not preempt - only a free kill does") {
    // Challenger needs two volleys, incumbent three: hold the finish.
    val current = Some(bid(165, reach = 2, cost = 3))
    val cheaper = Seq(bid(165, reach = 2, cost = 3), bid(182, reach = 3, cost = 2))

    assert(SharedFocus.hold(current, cheaper).contains(bid(165, reach = 2, cost = 3)))
  }
}
