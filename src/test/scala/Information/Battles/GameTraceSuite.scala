package Information.Battles

import cherryvis.{GameSummary, GameTrace, StringTable}
import org.scalatest.funsuite.AnyFunSuite

/**
 * The trace is the contract with the CherryVis viewer: exact JSON shapes,
 * first-logged-wins legend ids and change-only rows. Every format rule here
 * is pinned so a refactor cannot silently break the viewer.
 */
class GameTraceSuite extends AnyFunSuite {

  test("trace carries every viewer section exactly once") {
    val json = new GameTrace().toTraceJson(true)
    Seq("board_updates", "custom_names", "draw_commands", "game_values",
      "heatmaps", "logs", "tasks", "tensors_summaries", "trees", "types_names",
      "units_first_seen", "unit_ids", "units_logs", "units_updates", "units_tooltips"
    ).foreach(section => assert(json.contains("\"" + section + "\""), section))
  }

  test("legend follows runtime first-logged order") {
    val trace = new GameTrace()
    trace.logManager(10, 1, "IdleWorker")
    trace.logManager(20, 1, "GatherResources")
    val json = trace.toTraceJson(false)
    assert(json.contains("\"manager\": {\"10000\":\"IdleWorker\",\"10001\":\"GatherResources\"}"), json.slice(0, 200))
    assert(json.contains("\"manager\":10000"), json)
  }

  test("manager-only units render without the tooltip channel") {
    val trace = new GameTrace()
    trace.logManager(10, 1, "CombatManager")
    trace.logTooltip(10, 1, "ClusterForm")
    val updates = trace.buildUnitsUpdates(new StringTable, false)
    assert(updates == "\"1\":{\"10\":{\"manager\":10000}}", updates)
  }

  test("tooltip rows are compact frame-id pairs, one per transition") {
    val trace = new GameTrace()
    trace.logTooltip(10, 1, "ClusterForm")
    trace.logTooltip(20, 1, "ClusterForm")
    trace.logTooltip(30, 1, "ClusterKite")
    val table = new StringTable
    assert(trace.buildTooltips(table) == "\"1\":[[10,10000],[30,10001]]")
    assert(table.get("ClusterKite") == 10001)
  }

  test("free-text lines escape quotes and backslashes") {
    val trace = new GameTrace()
    trace.logUnitLine(10, 7, "Say \"hi\" \\ ok")
    val json = trace.buildUnitLogs()
    assert(json.contains("\\\"hi\\\""), json)
    assert(json.contains("\\\\"), json)
  }

  test("first-seen starts at frame 1 and pairs engine ids") {
    val trace = new GameTrace()
    trace.firstSeen.record(0, 5, 65, 100, 200)
    trace.firstSeen.noteEngineId(5, 99)
    assert(trace.firstSeen.toJson.contains("\"1\":[{\"id\":5"), trace.firstSeen.toJson)
    assert(trace.firstSeen.engineIdsJson == "[5,99]")
  }

  test("game summary names both sides and the result") {    val summary = new GameSummary
    summary.map = "Tau Cross"
    summary.gameDurationFrames = 15000
    summary.playerName = "Us"
    summary.playerRace = "Protoss"
    summary.playerWin = true
    summary.enemyName = "Them"
    summary.enemyRace = "Zerg"
    summary.enemyWin = false
    val json = summary.toJson
    assert(json.contains("\"map\": \"Tau Cross\""))
    assert(json.contains("\"p0_win\": true"))
    assert(json.contains("\"p1_win\": false"))
    assert(json.contains("\"game_duration_frames\": 15000"))
  }

  test("a managerless unit still names what it does when tooltips are on") {
    val trace = new GameTrace
    trace.logTooltip(10, 9, "ClusterKite")
    val table = new StringTable
    val updates = trace.buildUnitsUpdates(table, true)
    assert(updates == "\"9\":{\"10\":{\"tooltip\":10000}}", updates)
  }

  test("an empty trace still renders every section") {
    val json = new GameTrace().toTraceJson(false)
    assert(json.contains("\"board_updates\": {}"))
    assert(json.contains("\"units_updates\": {}"))
    assert(json.contains("\"units_logs\": {}"))
    assert(json.contains("\"logs\": []"))
    assert(json.contains("\"unit_ids\": []"))
  }

  test("units sort by id in every per-unit section") {
    val trace = new GameTrace
    trace.logManager(10, 30, "B")
    trace.logManager(10, 5, "A")
    val table = new StringTable
    val updates = trace.buildUnitsUpdates(table, true)
    assert(updates.indexOf("\"5\"") < updates.indexOf("\"30\""), updates)
  }
}
