package Debugging.CherryVis

import Lifecycle.With
import Information.Battles.ArmyStrength
import ProxyBwapi.UnitInfo.UnitInfo
import cherryvis.{GameSummary, GameTrace}

import scala.collection.mutable

/**
 * CherryVis trace facade: per-unit ownership/behavior history plus the viewer
 * trace file, written exactly once at game end.
 *
 * The format and the in-memory accumulation live in the dependency-free
 * `cherryvis` library (3rdparty/cherryvis); this object is the only bot-side
 * layer. It translates game objects to primitives (ids, names, frames) and
 * owns the output directory. When disabled, every entry point returns after
 * one flag read, so an untraced game pays nothing.
 */
object CherryVis {
  private val directoryName = "cherryvis_dir.rep.cvis"

  private var trace: GameTrace = new GameTrace
  private val lastOwner     = new mutable.HashMap[Int, String]
  private val lastOwnerRef  = new mutable.HashMap[Int, AnyRef]
  private val lastBehavior  = new mutable.HashMap[Int, String]
  private val lastBehaviorRef = new mutable.HashMap[Int, AnyRef]
  private val lastBoard     = new mutable.HashMap[String, String]
  private val lastOrders    = new mutable.HashMap[Int, String]
  private val lastDecision  = new mutable.HashMap[Int, String]
  private val knownUnits    = new mutable.HashSet[Int]
  private val rearmedUnits  = new mutable.HashSet[Int]
  private var lastStrategy  = ""

  def enabled: Boolean = With.configuration != null && With.configuration.cherryVis && ! With.umsMode

  def initialize(): Unit = {
    trace = new GameTrace
    lastOwner.clear()
    lastOwnerRef.clear()
    lastBehavior.clear()
    lastBehaviorRef.clear()
    lastBoard.clear()
    lastOrders.clear()
    lastDecision.clear()
    knownUnits.clear()
    rearmedUnits.clear()
    lastStrategy = ""
  }

  def update(): Unit = {
    if ( ! enabled) return

    noteFirstSeen(With.units.ours)
    noteFirstSeen(With.units.neutral)
    noteOrders(With.units.ours)
    val strategy = With.strategy.strategiesSelected.toSeq.map(_.toString).sorted.mkString("+")
    if (strategy != lastStrategy) {
      lastStrategy = strategy
      trace.logState(With.frame, "Strategy", strategy)
    }
    // Global strength next to the AttackStance row: display only, the
    // withdraw decision itself runs on per-unit sim verdicts.
    board("ArmyStrength", ArmyStrength.score(
      With.battles.globalAttack.us.skimStrengthTotal,
      With.battles.globalAttack.enemy.skimStrengthTotal).toInt.toString)
  }

  /**
   * Who drives the unit (the tactic/client that claimed it via intend()).
   * Change-only: steady ownership costs one map lookup, never a row
   * (same client object short-circuits before any string is built).
   */
  def noteOwner(unitId: Int, owner: Any): Unit = {
    if ( ! enabled) return

    val ref = owner.asInstanceOf[AnyRef]
    if (lastOwnerRef.getOrElse(unitId, null) eq ref) return

    lastOwnerRef(unitId) = ref
    val name = shortName(owner)
    if (lastOwner.getOrElse(unitId, "") != name) {
      lastOwner(unitId) = name
      trace.logManager(With.frame, unitId, name)
    }
  }

  /**
   * What the unit is doing (the top-level intent action).
   * Change-only like the owner above.
   */
  def noteBehavior(unitId: Int, behavior: Any): Unit = {
    if ( ! enabled) return

    val ref = behavior.asInstanceOf[AnyRef]
    if (lastBehaviorRef.getOrElse(unitId, null) eq ref) return

    lastBehaviorRef(unitId) = ref
    val name = shortName(behavior)
    if (lastBehavior.getOrElse(unitId, "") != name) {
      lastBehavior(unitId) = name
      trace.logTooltip(With.frame, unitId, name)
    }
  }

  def board(key: String, value: String): Unit = {
    if ( ! enabled) return

    if (lastBoard.getOrElse(key, "") != value) {
      lastBoard(key) = value
      trace.logState(With.frame, key, value)
    }
  }

  /**
   * Why the unit is doing what it is doing (Stardust `CherryVis::log(id)`).
   * Change-only like owner/behavior above: a steady reason costs one map
   * lookup, never a row, so per-frame call sites stay free in traced games.
   */
  def decision(unitId: Int, reason: String): Unit = {
    if ( ! enabled) return

    if (lastDecision.getOrElse(unitId, "") != reason) {
      lastDecision(unitId) = reason
      trace.logUnitLine(With.frame, unitId, reason)
    }
  }

  def log(message: String): Unit = {
    if ( ! enabled) return

    trace.logGlobal(With.frame, message, "Unknown")
  }

  def managerRows(unitId: Int): Map[Int, String] = {
    import scala.collection.JavaConverters._
    trace.managerRows(unitId).asScala.map(pair => (pair._1.intValue, pair._2)).toMap
  }

  def finish(isWinner: Boolean): Unit = {
    if ( ! enabled) return

    try {
      val summary = new GameSummary
      summary.gameDurationFrames  = With.frame
      summary.map                 = With.mapFileName
      summary.playerName          = With.self.name
      summary.playerRace          = With.self.raceCurrent.toString
      summary.playerWin           = isWinner
      summary.enemyName           = With.enemy.name
      summary.enemyRace           = With.enemy.raceCurrent.toString
      summary.enemyWin            = ! isWinner

      new java.io.File(With.bwapiData.write + "/" + directoryName).mkdirs()
      val prefix = directoryName + "/"
      val content = trace.toTraceJson(true)
      With.bwapiData.writeToFile(prefix + "trace.json", content)
      With.bwapiData.writeToFile(prefix + "trace_raw.json", content)
      With.bwapiData.writeToFile(prefix + "game_summary.json", summary.toJson)
    } catch { case exception: Exception => With.logger.onException(exception) }
  }

  /**
   * First-seen records feed the viewer match (same type within 25px at
   * the recorded frame, no retry). A mobile unit at completion still
   * reports its producer position - a crowd the viewer matches nothing
   * in, so every late unit reads "i???" while frame-0 seeds match.
   * Statics record at once (position never changes); mobiles defer seven
   * frames past completion until walked out with a position of their own.
   * Dead or gone by then drop silently. Capability is read off the class,
   * never the state: canMove needs aliveAndComplete, so a training unit
   * would take the immediate branch at its producer. Atlantis NEXT #68.
   */
  private def noteFirstSeen(units: Iterable[UnitInfo]): Unit = {
    units.foreach(unit => {
      if (knownUnits.add(unit.id)) {
        if ( ! unit.unitClass.canMove) recordFirstSeen(unit)
        else trace.firstSeen.defer(unit.id, With.frame)
      } else if (unit.complete && unit.unitClass.canMove && rearmedUnits.add(unit.id)) {
        // Completion re-arms the deferral: a unit training past the first
        // window would otherwise drop out of pending and never record.
        trace.firstSeen.defer(unit.id, With.frame)
      }
    })
    import scala.collection.JavaConverters._
    trace.firstSeen.pollDue(With.frame).asScala.foreach(id => {
      With.units.getId(id).filter(u => u.alive && u.complete).foreach(recordFirstSeen)
    })
  }

  private def recordFirstSeen(unit: UnitInfo): Unit = {
    trace.firstSeen.record(With.frame, unit.id, unit.unitClass.id, unit.pixel.x, unit.pixel.y)
    trace.firstSeen.noteEngineId(unit.id, unit.id)
  }

  /**
   * Engine-order narrative, one row per change: `#Move@0:12`.
   * Observes only (like the Atlantis OrderWatcher): it never issues orders,
   * so it cannot perturb the behavior it reports. Change-only keeps a traced
   * game at a handful of rows per unit per game.
   */
  private def noteOrders(units: Iterable[UnitInfo]): Unit = {
    units.foreach(unit => {
      if ( ! unit.unitClass.isBuilding) {
        val order = Option(unit.bwapiUnit.getLastCommand).map(_.getType.name).getOrElse("NO_COMMAND")
        if (lastOrders.getOrElse(unit.id, "") != order) {
          lastOrders(unit.id) = order
          trace.logUnitLine(With.frame, unit.id, f"#$order@${minSec(With.frame)}")
        }
      }
    })
  }

  private def minSec(frame: Int): String = {
    val seconds = frame / 24
    f"${seconds / 60}:${seconds % 60}%02d"
  }

  private def shortName(value: Any): String = {
    if (value == null) return ""

    value.getClass.getSimpleName.stripSuffix("$")
  }
}

