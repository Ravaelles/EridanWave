package Debugging

import Lifecycle.With
import java.io.File

/** Split from CombatVisIO: one IO role per file. */
object CombatVisVisualizer {

  @volatile private[Debugging] var launched: Boolean = false
  @volatile private[Debugging] var visProcess: Process = _

  def launchVisualizer(): Unit = {
    try {
      CombatVisMapExport.ensureDirs()
      val path = CombatVisThreads.opponentFile.getAbsolutePath
      // Prefer launching an external Java process to isolate UI from the bot JVM
      val cp = System.getProperty("java.class.path", ".")
      val pb = new ProcessBuilder("java", "-cp", cp, "Debugging.EridanSimViz", path)
      pb.directory(CombatVisThreads.baseDir)
      pb.redirectErrorStream(true)
      visProcess = pb.start()
      launched = true
    } catch { case exception: Exception => With.logger.quietlyOnException(exception) }
  }

  def terminateVisualizer(): Unit = {
    try {
      if (launched) {
        if (visProcess != null) {
          try { visProcess.destroy() } catch { case _: Throwable => }
          visProcess = null
        } else {
          try Debugging.EridanSimViz.requestClose() catch { case exception: Exception => With.logger.quietlyOnException(exception) }
        }
      }
    } catch { case exception: Exception => With.logger.quietlyOnException(exception) } finally { launched = false }
  }
}
