package Micro.Actions.Combat.Decisioning

import Utilities.DispatchBand
import Utilities.DispatchBand.{High, Medium}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Technique dispatch order: one explicit list, first claim wins. Every
 * row pins the (name, band) sequence the pipeline serves - reorder by
 * moving a registry line, disable by commenting one out. Behavior lives
 * in the stages; this suite owns the arbitration.
 */
class TechniqueDispatchOrderSuite extends AnyFunSuite {

  test("the pipeline serves survival before duels before leftovers") {
    val order = TechniquePipeline.stages.map(s => (s.name, s.band))
    assert(order.map(_._1) == Vector(
      "Avoid",
      "StaticKeepOff",
      "Walk",
      "Aim",
      "Dodge",
      "Potshot",
      "AlwaysAttack",
      "DragoonStand",
      "DragoonCarrier",
      "Rearguard",
      "Retreat",
      "Brawl",
      "Abuse",
      "SuperKite"))
    assert(order.map(_._2) == Vector(
      High,
      High,
      Medium,
      Medium,
      Medium,
      Medium,
      High,
      Medium,
      Medium,
      Medium,
      High,
      Medium,
      Medium,
      Medium))
  }
}
