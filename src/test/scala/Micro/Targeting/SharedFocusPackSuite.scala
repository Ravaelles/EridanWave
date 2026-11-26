package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class SharedFocusPackSuite extends AnyFunSuite {

  private def bid(id: Int, reach: Int, cost: Int): SharedFocus.Bid =
    SharedFocus.Bid(id, reach, cost)

  test("an empty pack seeds from the choose order") {
    val bids = Seq(bid(182, reach = 4, cost = 3), bid(165, reach = 2, cost = 5), bid(177, reach = 3, cost = 2))

    assert(SharedFocus.orderPack(Vector.empty, bids, max = 3) == Vector(182, 177, 165))
  }

  test("a committed pack holds through reach churn") {
    // Reach flickers 3<->1 while nothing dies: order must not move.
    val current = Vector(165, 182)
    val churned = Seq(bid(165, reach = 1, cost = 3), bid(182, reach = 4, cost = 3))

    assert(SharedFocus.orderPack(current, churned, max = 3) == Vector(165, 182))
  }

  test("dead picks drop out, the rest hold their order") {
    assert(SharedFocus.orderPack(Vector(165, 182), Seq(bid(182, reach = 2, cost = 3)), max = 3) == Vector(182))
    assert(SharedFocus.orderPack(Vector(165), Seq.empty, max = 3).isEmpty)
  }

  test("a one-volley free kill jumps the queue") {
    val current = Vector(165, 182)
    val freeKill = Seq(bid(165, reach = 2, cost = 4), bid(182, reach = 2, cost = 3), bid(177, reach = 3, cost = 1))

    assert(SharedFocus.orderPack(current, freeKill, max = 3) == Vector(177, 165, 182))
  }

  test("a cheaper grind does not jump the queue") {
    val current = Vector(165, 182)
    val cheaper = Seq(bid(165, reach = 2, cost = 3), bid(182, reach = 2, cost = 2))

    assert(SharedFocus.orderPack(current, cheaper, max = 3) == Vector(165, 182))
  }

  test("newcomers append in choose order behind the committed pack") {
    val current = Vector(165)
    val plusNew = Seq(bid(165, reach = 2, cost = 3), bid(177, reach = 5, cost = 4), bid(182, reach = 3, cost = 2))

    assert(SharedFocus.orderPack(current, plusNew, max = 3) == Vector(165, 177, 182))
  }

  test("the list caps at the pack size, worst first out") {
    val bids = (1 to 5).map(i => bid(i, reach = 5 - i, cost = i))

    assert(SharedFocus.orderPack(Vector.empty, bids, max = 3) == Vector(1, 2, 3))
  }
}
