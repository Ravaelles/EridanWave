package Placement

import Lifecycle.With
import java.nio.file.{AtomicMoveNotSupportedException, Files, Paths, StandardCopyOption}
import java.nio.file.{AtomicMoveNotSupportedException, Files, Paths, StandardCopyOption}

object PlacementCache {

  def load(placement: Placement): Option[Boolean] = {
    if (PlacementCacheKey.revision.isEmpty) return None

    val expectedRevision = PlacementCacheKey.revision.get
    val expectedKey = PlacementCacheKey.key
    val directories = Vector(With.bwapiData.read, With.bwapiData.write, With.bwapiData.ai)
    var rejected = false
    var directoryIndex = 0
    while (directoryIndex < directories.length) {
      val file = Paths.get(directories(directoryIndex), expectedKey.filename).toFile
      if (file.isFile) {
        PlacementCacheFormat.read(file, expectedRevision, expectedKey) match {
          case PlacementCacheFormat.Hit(data) =>
            try {
              PlacementCacheFormat.restore(placement, data)
              placement.finishCachedRestore()
              With.logger.debug(f"Loaded ${data.fits.length} building-placement fits from ${file.getAbsolutePath}")

              return Some(rejected && directoryIndex != 1)
            } catch { case exception: Exception =>
              rejected = true
              With.logger.warn(f"Ignoring invalid building-placement cache ${file.getAbsolutePath}: ${exception.getMessage}")
            }
          case PlacementCacheFormat.Stale =>
            rejected = true
            With.logger.debug(f"Ignoring building-placement cache from another revision: ${file.getAbsolutePath}")
          case PlacementCacheFormat.Invalid(reason) =>
            rejected = true
            With.logger.warn(f"Ignoring invalid building-placement cache ${file.getAbsolutePath}: $reason")
        }
      }

      directoryIndex += 1
    }
    None
  }

  def write(placement: Placement): Unit = {
    if (PlacementCacheKey.revision.isEmpty) return

    val cacheKey = PlacementCacheKey.key
    val directory = Paths.get(With.bwapiData.write)
    val target = directory.resolve(cacheKey.filename)
    var temporary: java.nio.file.Path = null
    try {
      Files.createDirectories(directory)
      temporary = Files.createTempFile(directory, cacheKey.filename + ".", ".tmp")
      PlacementCacheFormat.write(temporary.toFile, PlacementCacheKey.revision.get, cacheKey, placement)
      try {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
      } catch { case _: AtomicMoveNotSupportedException =>
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
      }
      temporary = null
      With.logger.debug(f"Wrote building-placement cache to ${target.toFile.getAbsolutePath}")
    } catch { case exception: Exception =>
      With.logger.warn(f"Failed to write building-placement cache ${target.toFile.getAbsolutePath}: ${exception.getMessage}")
    } finally {
      if (temporary != null) {
        try Files.deleteIfExists(temporary) catch { case _: Exception => }
      }
    }
  }
}
