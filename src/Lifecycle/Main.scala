package Lifecycle

import Lifecycle.Launch.{GameLauncherFactory, LaunchConfig}
import Mathematics.Maff
import Utilities.?
import bwapi.BWClientConfiguration

import java.lang.management.ManagementFactory
import scala.collection.JavaConverters._

object Main {

  private val JBWAPIClientDataSizeBytes =  33017048
  private val botRequiredDataSizeBytes  = 650 * 1000 * 1000
  private val memoryFree  : Long            = Math.min(Runtime.getRuntime.maxMemory, Runtime.getRuntime.totalMemory)
  val jvmRuntimeArguments : Vector[String]  = ManagementFactory.getRuntimeMXBean.getInputArguments.asScala.toVector
  val liveDebugging       : Boolean         = jvmRuntimeArguments.exists(arg => arg.contains("eridandebug") || arg.contains("purpledebug"))
  val framesBufferable    : Int             = ((memoryFree - botRequiredDataSizeBytes) / JBWAPIClientDataSizeBytes).toInt
  val useFrameBuffer      : Boolean         = ! liveDebugging
  val framesToBuffer      : Int             = ?(useFrameBuffer, Maff.clamp(framesBufferable, 1, 10), 0)

  val jbwapiConfiguration: BWClientConfiguration = new BWClientConfiguration()
    .withAutoContinue(false)
    .withMaxFrameDurationMs(30)
    .withAsyncFrameBufferCapacity(framesToBuffer)
    .withAsync(useFrameBuffer)
    .withAsyncUnsafe(useFrameBuffer)
    .withDebugConnection(true)

  def main(args: Array[String]): Unit = {
    System.out.println(f"EridanWave Main.main() invoked ${System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean.getStartTime}ms after VM start.")
    val launch = LaunchConfig.parse(args)
    if (launch.isHelpRequested) {
      LaunchConfig.printHelp()

      return
    }
    // Explicit --cherryvis/--no-cherryvis wins over the config file and the
    // environment. A system property is the in-process channel (same JVM);
    // the Wine path travels separately via wine-run.sh -> ERIDANWAVE_CHERRYVIS.
    if (launch.isCherryVisExplicit) {
      System.setProperty("eridanwave.cherryvis", launch.isCherryVis.toString)
    }
    // Tuning strategy and per-game param overrides travel the same way:
    // system properties in-process, -D flags on the Wine client JVM.
    if (launch.getTuningStrategy != null && launch.getTuningStrategy.nonEmpty) {
      System.setProperty("eridanwave.tuningStrategy", launch.getTuningStrategy)
    }
    launch.getTuneParams.asScala.foreach { case (key, value) =>
      System.setProperty("eridanwave.tune." + key, value)
    }
    GameLauncherFactory.forConfig(launch).launch(launch)
    try {
      EridanBWClient.startGame(jbwapiConfiguration)
    } catch {
      // Killing the game mid-frame (Esc, supervisor cleanup) drops the BWAPI
      // server while the client is blocked reading it. That is an operator
      // shutdown, not a bug: say so instead of dumping an EOF stack trace.
      case e: java.io.EOFException =>
        System.out.println("[launcher] Game connection lost (the game was probably killed). Shutting down.")
    }
  }
}

