package Debugging

import Information.Battles.Types.Battle
import scala.collection.mutable.ArrayBuffer

/**
 * Combat visualizer IO: thin facade, one IO role per delegate.
 * Threads owns queues and throttle state, MapExport writes map data,
 * SimLog writes simulation logs, Visualizer owns the viewer process.
 */
object CombatVisIO {

  def ensureDirs(): Unit = CombatVisMapExport.ensureDirs()
  def exportMapData(): Unit = CombatVisMapExport.exportMapData()
  def prepareEmptySimFile(): Unit = CombatVisMapExport.prepareEmptySimFile()
  def writeSimulationLogAsync(battle: Battle, framesLog: ArrayBuffer[String], startGameFrame: Int): Unit =
    CombatVisSimLog.writeSimulationLogAsync(battle, framesLog, startGameFrame)
  def writeCompressedSimDumpIfNeededAsync(battle: Battle, framesLog: ArrayBuffer[String], startGameFrame: Int): Unit =
    CombatVisSimLog.writeCompressedSimDumpIfNeededAsync(battle, framesLog, startGameFrame)
  def launchVisualizer(): Unit = CombatVisVisualizer.launchVisualizer()
  def terminateVisualizer(): Unit = CombatVisVisualizer.terminateVisualizer()
}
