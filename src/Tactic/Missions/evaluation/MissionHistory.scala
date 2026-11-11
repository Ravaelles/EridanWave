package Tactic.Missions.evaluation

import Information.Battles.KillLedger
import Lifecycle.With
import Macro.Facts.MacroCounting
import Tactic.Missions.core.Mission

import scala.collection.mutable

/**
 * Mission ledger, evaluated once at game end: every launch gets one row
 * (mission, enemy, map, verdict, profit, lifetime, supply, won, harass)
 * in mission-history.csv next to the game history. Profit is the global
 * kill window over the span, booked in full to every concurrent mission;
 * supply is global army supply, not mission cost. Lifetime runs from
 * launch to the last frame any member was alive (rows before Nov 2026
 * measured to terminate instead - same file, no version column, so the
 * repeat gate averages both definitions). Override evaluateMission per
 * mission for custom rules; the default verdict is MissionVerdict.
 */
class MissionHistory extends MacroCounting {

  private case class Span(
    mission     : String,
    launchFrame : Int,
    killBase    : Int,
    lossBase    : Int,
    supplyBase  : Int,
    var endFrame: Int,
    var harassFrames: Int = 0)

  private val spans = new mutable.HashMap[Mission, Vector[Span]]

  /** Past launches per mission, loaded once from mission-history.csv. */
  private lazy val pastRuns: Map[String, Vector[MissionRepeatGate.PastRun]] = {
    try {
      val file = With.bwapiData.write + "/mission-history.csv"
      val source = scala.io.Source.fromFile(file)
      try MissionRepeatGate.groupRows(source.getLines().toVector)
      finally source.close()
    } catch { case _: Exception => Map.empty }
  }

  def repeatAllowed(mission: Mission): Boolean =
    MissionRepeatGate.repeatAllowed(
      pastRuns.getOrElse(mission.getClass.getSimpleName.stripSuffix("$"), Vector.empty))

  def recordLaunch(mission: Mission): Unit = {
    spans(mission) = spans.getOrElse(mission, Vector.empty) :+ Span(
      mission.getClass.getSimpleName.stripSuffix("$"),
      With.frame,
      KillLedger.killedResources,
      KillLedger.lostResources,
      supplyUsed200,
      With.frame)
  }

  def recordEnd(mission: Mission): Unit = {
    // Advance only with live members: a terminate lagging a wipe must not
    // re-open the last-alive freeze (a recall with live members advances).
    if (mission.units.exists(_.alive)) {
      spans.get(mission).foreach(_.lastOption.foreach(_.endFrame = With.frame))
    }
  }

  /**
   * Per-frame span upkeep while launched: the end frame tracks the last
   * frame any member was alive (last-member lifetime precision - terminate
   * may lag deaths by hundreds of frames, or never come before game end),
   * plus one denial frame parked on enemy workers.
   */
  private var lastTickFrame: Int = -1

  def onFrame(): Unit = {
    // Once per frame advance: onFrame keeps arriving while paused with the
    // engine frozen, and denial must not farm a static frame.
    if (spans.isEmpty || With.frame == lastTickFrame) return

    lastTickFrame = With.frame
    val workers = ProxyBwapi.UnitTracking.AliveEnemies.get().filter(_.unitClass.isWorker)

    spans.foreach { case (mission, spanList) =>
      val span = spanList.lastOption
      span.foreach(s => {
        if (mission.launched && mission.units.exists(_.alive)) {
          s.endFrame = With.frame
          if (workers.nonEmpty && mission.units.exists(u => u.alive &&
            workers.exists(w => MissionHarass.denying(u.pixelDistanceEdge(w) / 32.0)))) {
            s.harassFrames += 1
          }
        }
      })
    }
  }

  def onEnd(wonGame: Boolean): Unit = {
    try {
      val rows = spans.toVector.flatMap { case (_, spanList) => spanList.map { span =>
        val profit = (KillLedger.killedResources - span.killBase) - (KillLedger.lostResources - span.lossBase)
        val lifetime = Math.max(0, span.endFrame - span.launchFrame)
        val verdict = evaluateMission(span.mission, profit, lifetime, wonGame, span.harassFrames)
        f"${System.currentTimeMillis},${With.history.currentMapName.replace(",", "")},${With.history.currentEnemyName.replace(",", "")},${span.mission},${MissionVerdict.name(verdict)},$profit,$lifetime,${span.supplyBase},$wonGame,${span.harassFrames}"
      }}
      if (rows.nonEmpty && ! With.umsMode) {
        val file = With.bwapiData.write + "/mission-history.csv"
        val previous = try {
          val source = scala.io.Source.fromFile(file)
          try source.getLines().toVector
          finally source.close()
        } catch { case _: Exception => Vector.empty }
        With.bwapiData.writeToFile("mission-history.csv", (previous ++ rows).mkString("\n"))
      }
    } catch { case exception: Exception => With.logger.onException(exception) }
    spans.clear()
  }

  protected def evaluateMission(mission: String, profit: Int, lifetimeFrames: Int, wonGame: Boolean, workerHarassFrames: Int = 0): MissionVerdict.Verdict =
    MissionVerdict.evaluate(profit, lifetimeFrames, wonGame, workerHarassFrames)
}
