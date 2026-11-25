package Micro.Agency

import org.scalatest.funsuite.AnyFunSuite

class IssuedOrderFuseSuite extends AnyFunSuite {

  test("the first issuance in a frame claims it") {
    val (stored, duplicate) = IssuedOrderFuse.tryClaim(None, frame = 100, OrderIssuer.MoveStep)

    assert( ! duplicate)
    assert(stored.contains(IssuedOrderFuse.Claim(100, OrderIssuer.MoveStep)))
  }

  test("a second issuance in the same frame is a duplicate") {
    val (first, _) = IssuedOrderFuse.tryClaim(None, 100, OrderIssuer.MoveStep)
    val (stored, duplicate) = IssuedOrderFuse.tryClaim(first, 100, OrderIssuer.EngageAttack)

    assert(duplicate)
    assert(stored.contains(IssuedOrderFuse.Claim(100, OrderIssuer.MoveStep)))
  }

  test("the same issuer re-issuing in the same frame is still a duplicate") {
    val (first, _) = IssuedOrderFuse.tryClaim(None, 100, OrderIssuer.MoveStep)
    val (_, duplicate) = IssuedOrderFuse.tryClaim(first, 100, OrderIssuer.MoveStep)

    assert(duplicate)
  }

  test("a new frame releases the fuse without any reset") {
    val (first, _) = IssuedOrderFuse.tryClaim(None, 100, OrderIssuer.MoveStep)
    val (stored, duplicate) = IssuedOrderFuse.tryClaim(first, 101, OrderIssuer.EngageAttack)

    assert( ! duplicate)
    assert(stored.contains(IssuedOrderFuse.Claim(101, OrderIssuer.EngageAttack)))
  }

  test("the duplicate key names first-vs-second at the class") {
    assert(IssuedOrderFuse.duplicateKey(OrderIssuer.MoveStep, OrderIssuer.EngageAttack, "Dragoon") ==
      "duplicateOrder.CommanderMove.moveVsCommanderEngage.issueAttack.at.Dragoon")
  }
}
