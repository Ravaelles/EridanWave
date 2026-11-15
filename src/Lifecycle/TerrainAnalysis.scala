package Lifecycle

import Lifecycle.Launch.BwapiIni
import bwapi.Game
import bwta.BWTA

/**
 * Terrain analysis policy (one home for the BWTA retry ladder): UMS micro
 * maps skip the doomed analysis outright, live maps retry once with
 * assertions off, and a failed retry plays without analyzed terrain
 * instead of dying on frame zero. Takes the game and the map name;
 * logging rides the usual channels.
 */
object TerrainAnalysis {

  def analyze(game: Game, mapFileName: String): Unit = {
    // UMS micro maps have no bases by design, so BWTA can never finish on them.
    // Skip the doomed analysis outright instead of flooding the log with stacks.
    if (mapFileName != null && BwapiIni.isUmsMap(mapFileName)) {
      With.logger.debug("Skipping terrain analysis (UMS map).")
      System.out.println("[launcher] Skipping terrain analysis (UMS map).")

      return
    }

    try {
      BWTA.readMap(game)
      BWTA.analyze()
    } catch { case exception: Exception =>
      With.logger.quietlyOnException(exception)
      With.logger.debug("Retrying terrain analysis with assertions disabled.")

      // A failed retry must not abort the start: play without analyzed
      // terrain instead of dying on frame zero. Only a one-liner is logged;
      // the first attempt's stack stays in the log buffer for forensics.
      try {
        BWTA.readMap(game)
        BWTA.setFailOnError(false)
        BWTA.analyze()
      } catch { case retryException: Exception =>
        With.logger.debug("Terrain analysis unavailable on this map; continuing without it.")
        System.out.println("[launcher] Terrain analysis unavailable on this map; continuing without it.")
      }
    }
  }
}
