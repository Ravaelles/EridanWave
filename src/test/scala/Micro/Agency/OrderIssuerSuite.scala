package Micro.Agency

import org.scalatest.funsuite.AnyFunSuite

class OrderIssuerSuite extends AnyFunSuite {

  test("every door has a distinct stable key") {
    val keys = OrderIssuer.all.map(_.key)
    assert(keys.distinct.size == keys.size)
    assert(keys.forall(k => k.startsWith("Commander") || k.startsWith("Approach")))
  }

  test("the ledger keeps the old call-site names") {
    assert(OrderIssuer.MoveStep.key == "CommanderMove.move")
    assert(OrderIssuer.EngageAttack.key == "CommanderEngage.issueAttack")
    assert(OrderIssuer.FormationHold.key == "ApproachEnemyFormation.hold")
  }
}
