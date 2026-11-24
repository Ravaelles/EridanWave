package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

/**
 * Behavior contract registry: every dispatch stage in every pipeline
 * names the gate suite that pins its unit contract. Add a stage,
 * add a row - the set-equality below fails on either side until both
 * match. Suicide suites assert the behavior; gate suites assert WHO
 * the behavior may ever apply to.
 */
class BehaviorCoverageSuite extends AnyFunSuite {

  private val technique: Map[String, Seq[String]] = Map(
    "Avoid" -> Seq("Micro.Actions.Combat.Decisioning.TechniqueStageGatesSuite"),
    "StaticKeepOff" -> Seq("Micro.Actions.Combat.Decisioning.TechniqueStageGatesSuite"),
    "Walk" -> Seq("Micro.Actions.Combat.Decisioning.TechniqueStageGatesSuite"),
    "Aim" -> Seq("Micro.Actions.Combat.Decisioning.TechniqueStageGatesSuite"),
    "Dodge" -> Seq("Micro.Actions.Combat.Decisioning.TechniqueStageGatesSuite"),
    "Potshot" -> Seq("Micro.Actions.Combat.Decisioning.TechniqueStageGatesSuite"),
    "AlwaysAttack" -> Seq("Micro.Actions.Combat.Decisioning.TechniqueStageGatesSuite"),
    "DarkTemplarHunt" -> Seq("Micro.Actions.Combat.Decisioning.TechniqueStageGatesSuite"),
    "DragoonStand" -> Seq("Micro.Actions.Combat.Decisioning.TechniqueStageGatesSuite"),
    "DragoonCarrier" -> Seq("Micro.Actions.Combat.Decisioning.TechniqueStageGatesSuite"),
    "DragoonMineDefuse" -> Seq("Micro.Actions.Combat.Decisioning.TechniqueStageGatesSuite"),
    "Rearguard" -> Seq("Micro.Actions.Combat.Decisioning.TechniqueStageGatesSuite"),
    "Retreat" -> Seq("Micro.Actions.Combat.Decisioning.TechniqueStageGatesSuite"),
    "Brawl" -> Seq("Micro.Actions.Combat.Decisioning.TechniqueStageGatesSuite"),
    "Abuse" -> Seq("Micro.Actions.Combat.Decisioning.TechniqueStageGatesSuite"),
    "SuperKite" -> Seq("Micro.Actions.Combat.Decisioning.TechniqueStageGatesSuite"))

  private val engage: Map[String, Seq[String]] = Map(
    "ContainFire" -> Seq("Micro.Actions.Combat.Decisioning.EngageStageGatesSuite"),
    "AdvanceGate" -> Seq("Micro.Actions.Combat.Decisioning.EngageStageGatesSuite"),
    "ZealotLeash" -> Seq("Micro.Actions.Combat.Decisioning.EngageStageGatesSuite"),
    "ClusterRejoin" -> Seq(
      "Micro.Actions.Combat.Decisioning.EngageStageGatesSuite",
      "Tactic.Squads.Decisions.ClusterCohesionSuite"))

  private val duel: Map[String, Seq[String]] = Map(
    "Push" -> Seq("Micro.Actions.Combat.Decisioning.DuelStageGatesSuite"),
    "Scavenge" -> Seq("Micro.Actions.Combat.Decisioning.DuelStageGatesSuite"),
    "Formation" -> Seq("Micro.Actions.Combat.Decisioning.DuelStageGatesSuite"),
    "Close" -> Seq("Micro.Actions.Combat.Decisioning.DuelStageGatesSuite"),
    "Reposition" -> Seq(
      "Micro.Actions.Combat.Decisioning.DuelStageGatesSuite",
      "Micro.Actions.Combat.Decisioning.RepositionBehaviorSuite"))

  private val static: Map[String, Seq[String]] = Map(
    "shells" -> Seq(
      "Micro.Actions.Combat.Decisioning.StaticKeepOffGatesSuite",
      "Micro.Actions.Combat.Maneuvering.ArtilleryKeepOffSuite"),
    "keep-off" -> Seq(
      "Micro.Actions.Combat.Decisioning.StaticKeepOffGatesSuite",
      "Micro.Actions.Combat.Decisioning.ScoutStaticKeepOffSuite"),
    "stand" -> Seq(
      "Micro.Actions.Combat.Decisioning.StaticKeepOffGatesSuite",
      "Micro.Actions.Combat.Decisioning.StaticStandoffSuite"))

  private def checkRegistry(staged: Set[String], table: Map[String, Seq[String]], registry: String): Unit = {
    val missing = staged -- table.keySet
    assert(missing.isEmpty, s"new $registry stages without a gate row: ${missing.mkString(", ")}")
    val stale = table.keySet -- staged
    assert(stale.isEmpty, s"gate rows without a $registry stage: ${stale.mkString(", ")}")
    table.values.flatten.toSeq.distinct.foreach(Class.forName)
  }

  test("every technique stage states its unit contract") {
    checkRegistry(TechniquePipeline.stages.map(_.name).toSet, technique, "technique")
  }

  test("every engage stage states its unit contract") {
    checkRegistry(EngagePipeline.stages.map(_.name).toSet, engage, "engage")
  }

  test("every duel stage states its unit contract") {
    checkRegistry(EngagePipeline.duelStages.map(_.name).toSet, duel, "duel")
  }

  test("every static claim states its unit contract") {
    checkRegistry(HandleEnemyCombatBuildings.order.toSet, static, "static")
  }
}
