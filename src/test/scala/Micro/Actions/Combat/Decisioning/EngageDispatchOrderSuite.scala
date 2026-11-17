package Micro.Actions.Combat.Decisioning

import Utilities.DispatchBand
import Utilities.DispatchBand.{High, Low, Medium}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Engage dispatch order: one explicit place, first claim wins. Both
 * rows pin the (name, band) sequences the pipelines serve - reorder by
 * moving a registry line, disable by commenting one out. Behavior
 * lives in the stages and their helpers; this suite owns the
 * arbitration.
 */
class EngageDispatchOrderSuite extends AnyFunSuite {

  test("the engage gate serves contain, march, leash, rejoin, hunt") {
    assert(EngagePipeline.order.map(_._1) == Vector(
      "ContainFire",
      "AdvanceGate",
      "ZealotLeash",
      "ClusterRejoin",
      "Hunt"))
    assert(EngagePipeline.order.map(_._2) == Vector(
      High,
      Low,
      Medium,
      Medium,
      Medium))
  }

  test("the duel serves push, scavenge, close, reposition") {
    assert(EngagePipeline.duelOrder.map(_._1) == Vector(
      "Push",
      "Scavenge",
      "Close",
      "Reposition"))
    assert(EngagePipeline.duelOrder.map(_._2) == Vector(
      Medium,
      Medium,
      Medium,
      Low))
  }
}
