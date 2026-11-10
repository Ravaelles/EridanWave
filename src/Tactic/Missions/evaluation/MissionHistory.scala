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
    var endFrame: Int)

  private val spans = new mutable.HashMap[Mission, Span]

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

  def onEnd(wonGame: Boolean): Unit = {
    try {
      val rows = spans.toVector.map { case (_, span) =>
        val profit = (KillLedger.killedResources - span.killBase) - (KillLedger.lostResources - span.lossBase)
        val lifetime = Math.max(0, span.endFrame - span.launchFrame)
        val verdict = evaluateMission(span.mission, profit, lifetime, wonGame)
        f"${System.currentTimeMillis},${With.history.currentMapName},${With.history.currentEnemyName},${span.mission},${MissionVerdict.name(verdict)},$profit,$lifetime,${span.supplyBase},$wonGame"
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
