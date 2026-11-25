package Micro.Agency

import org.scalatest.funsuite.AnyFunSuite

class IssuedOrderFuseSuite extends AnyFunSuite {

  test("the first issuance in a frame claims it") {
    val (stored, duplicate) = IssuedOrderFuse.tryClaim(None, frame = 100, "CommanderMove.move")

    assert( ! duplicate)
    assert(stored.contains(IssuedOrderFuse.Claim(100, "CommanderMove.move")))
  }

  test("a second issuance in the same frame is a duplicate") {
    val (first, _) = IssuedOrderFuse.tryClaim(None, 100, "CommanderMove.move")
    val (stored, duplicate) = IssuedOrderFuse.tryClaim(first, 100, "CommanderEngage.issueAttack")

    assert(duplicate)
    assert(stored.contains(IssuedOrderFuse.Claim(100, "CommanderMove.move")))
  }

  test("the same issuer re-issuing in the same frame is still a duplicate") {
    val (first, _) = IssuedOrderFuse.tryClaim(None, 100, "CommanderMove.move")
    val (_, duplicate) = IssuedOrderFuse.tryClaim(first, 100, "CommanderMove.move")

    assert(duplicate)
  }

  test("a new frame releases the fuse without any reset") {
    val (first, _) = IssuedOrderFuse.tryClaim(None, 100, "CommanderMove.move")
    val (stored, duplicate) = IssuedOrderFuse.tryClaim(first, 101, "CommanderEngage.issueAttack")

    assert( ! duplicate)
    assert(stored.contains(IssuedOrderFuse.Claim(101, "CommanderEngage.issueAttack")))
  }

  test("the duplicate key names first-vs-second at the class") {
    assert(IssuedOrderFuse.duplicateKey("CommanderMove.move", "CommanderEngage.issueAttack", "Dragoon") ==
      "duplicateOrder.CommanderMove.moveVsCommanderEngage.issueAttack.at.Dragoon")
  }
}
