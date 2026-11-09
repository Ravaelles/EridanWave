package Placement

import Lifecycle.With
import bwapi.Race
import java.nio.file.Paths
import scala.io.Source

object PlacementCacheKey {

  case class CacheKey(mapHash: String, ourRace: Int, enemyRace: Int, startX: Int, startY: Int) {
    private def safe(value: String): String = value.replaceAll("[^A-Za-z0-9._-]", "_")
    private def raceName(id: Int): String = Race.values().find(_.id == id).map(_.toString.toLowerCase).getOrElse(id.toString)
    val filename: String = f"placement-${safe(mapHash)}-${raceName(ourRace)}-${raceName(enemyRace)}-$startX-$startY.bin.gz"
  }

  def key: CacheKey = {
    val start = With.self.startTile
    val enemyRace = With.enemies.headOption.map(_.raceCurrent).getOrElse(Race.None)
    CacheKey(With.game.mapHash, With.self.raceCurrent.id, enemyRace.id, start.x, start.y)
  }

  private[Placement] lazy val revision: Option[String] = {
    val file = Paths.get(With.bwapiData.ai, "revision.txt").toFile
    if ( ! file.isFile) {
      With.logger.warn(f"Placement caching disabled: revision file not found at ${file.getAbsolutePath}")
      None
    } else {
      var source: Source = null
      try {
        source = Source.fromFile(file, "UTF-8")
        val output = source.mkString.trim
        if (output.isEmpty) {
          With.logger.warn(f"Placement caching disabled: revision file is empty at ${file.getAbsolutePath}")
          None
        } else {
          Some(output)
        }
      } catch { case exception: Exception =>
        With.logger.warn(f"Placement caching disabled: failed to read ${file.getAbsolutePath}: ${exception.getMessage}")
        None
      } finally {
        if (source != null) source.close()
      }
    }
  }
}
