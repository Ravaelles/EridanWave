package Tactic.Missions.evaluation

import Information.Battles.KillLedger
import Lifecycle.With
import Macro.Facts.MacroCounting
import Tactic.Missions.core.Mission

import scala.collection.mutable

/**
 * Mission ledger, evaluated once at game end: every launched mission gets
 * one row (mission, enemy, map, verdict, profit, lifetime, supply, won) in
 * mission-history.csv next to the game history. Profit is the global kill
 * window over the mission span (no killer attribution yet: per-unit credit
 * arrives with KillCredits); lifetime is the mission span (last-member
 * precision needs death tracking). Override evaluateMission per mission
 * for custom rules; the default verdict is MissionVerdict.
 */
class MissionHistory extends MacroCounting {

  private case class Span(
    mission     : String,
    launchFrame : Int,
    killBase    : Int,
    lossBase    : Int,
    supplyBase  : Int,
    costBase    : Int,
    var endFrame: Int,
    var harassFrames: Int = 0)

  private val spans = new mutable.HashMap[Mission, Span]

  /** Past launches per mission, loaded once from mission-history.csv. */
  private lazy val pastRuns: Map[String, Vector[MissionRepeatGate.PastRun]] = {
    try {
      val file = With.bwapiData.write + "/mission-history.csv"
      scala.io.Source.fromFile(file).getLines().toVector
        .flatMap(MissionRepeatGate.parseRow)
        .groupBy(_._1).mapValues(_.map(_._2).toVector).toMap
    } catch { case _: Exception => Map.empty }
  }

  def repeatAllowed(mission: Mission): Boolean =
    MissionRepeatGate.repeatAllowed(
      pastRuns.getOrElse(mission.getClass.getSimpleName.stripSuffix("$"), Vector.empty))

  def recordLaunch(mission: Mission): Unit = {
    spans(mission) = Span(
      mission.getClass.getSimpleName.stripSuffix("$"),
      With.frame,
      KillLedger.killedResources,
      KillLedger.lostResources,
      supplyUsed200,
      mission.units.map(u => UnitBounty.of(u.unitClass)).sum,
      With.frame)
  }

  def recordEnd(mission: Mission): Unit = {
    spans.get(mission).foreach(span => span.endFrame = With.frame)
  }

  /** One denial frame per launched mission parked on enemy workers. */
  def onFrame(): Unit = {
    if (spans.isEmpty) return

    val workers = ProxyBwapi.UnitTracking.AliveEnemies.get().filter(_.unitClass.isWorker)
    if (workers.isEmpty) return

    spans.foreach { case (mission, span) =>
      if (mission.launched && mission.units.exists(u =>
        workers.exists(w => MissionHarass.denying(u.pixelDistanceEdge(w) / 32.0)))) {
        span.harassFrames += 1
      }
    }
  }

  def onEnd(wonGame: Boolean): Unit = {
    try {
      val rows = spans.toVector.map { case (_, span) =>
        val profit = (KillLedger.killedResources - span.killBase) - (KillLedger.lostResources - span.lossBase)
        val lifetime = Math.max(0, span.endFrame - span.launchFrame)
        val verdict = evaluateMission(span.mission, profit, lifetime, wonGame, span.harassFrames)
        f"${System.currentTimeMillis},${With.history.currentMapName},${With.history.currentEnemyName},${span.mission},${MissionVerdict.name(verdict)},$profit,$lifetime,${span.supplyBase},$wonGame,${span.harassFrames}"
      }
      if (rows.nonEmpty) {
        val file = With.bwapiData.write + "/mission-history.csv"
        val previous = try {
          scala.io.Source.fromFile(file).getLines().toVector
        } catch { case _: Exception => Vector.empty }
        With.bwapiData.writeToFile("mission-history.csv", (previous ++ rows).mkString("\n"))
      }
    } catch { case exception: Exception => With.logger.onException(exception) }
    spans.clear()
  }

  protected def evaluateMission(mission: String, profit: Int, lifetimeFrames: Int, wonGame: Boolean, workerHarassFrames: Int = 0): MissionVerdict.Verdict =
    MissionVerdict.evaluate(profit, lifetimeFrames, wonGame, workerHarassFrames)
}
