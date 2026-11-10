package Debugging

import Lifecycle.With
import java.io.{DataOutputStream, File, FileOutputStream}
import java.nio.charset.StandardCharsets

/** Split from CombatVisIO: one IO role per file. */
object CombatVisMapExport {

  def ensureDirs(): Unit = {
    try {
      if (!CombatVisThreads.baseDir.exists()) CombatVisThreads.baseDir.mkdirs()
      if (CombatVisThreads.gameStartEpochSeconds == 0L) {
        CombatVisThreads.gameStartEpochSeconds = System.currentTimeMillis() / 1000L
      }
    } catch { case exception: Exception => With.logger.quietlyOnException(exception) }
  }

  def exportMapData(): Unit = {
    try {
      ensureDirs()
      val hash = With.game.mapHash()
      val outFile = new File(CombatVisThreads.baseDir, hash + ".mapbin")
      if (outFile.exists()) return

      val dos = new DataOutputStream(new FileOutputStream(outFile))
      try {
        val tileW = With.mapTileWidth
        val tileH = With.mapTileHeight
        val walkW = With.mapWalkWidth
        val walkH = With.mapWalkHeight
        // Header
        dos.writeInt(tileW)
        dos.writeInt(tileH)
        dos.writeInt(walkW)
        dos.writeInt(walkH)
        // Ground heights per tile (byte per tile)
        var y = 0
        while (y < tileH) {
          var x = 0
          while (x < tileW) {
            val h = With.game.getGroundHeight(x, y) & 0xFF
            dos.writeByte(h)
            x += 1
          }
          y += 1
        }
        // Walkability per mini-tile (byte per walk tile)
        var wy = 0
        while (wy < walkH) {
          var wx = 0
          while (wx < walkW) {
            val w = if (With.game.isWalkable(wx, wy)) 1 else 0
            dos.writeByte(w)
            wx += 1
          }
          wy += 1
        }
      } finally {
        dos.close()
      }
    } catch { case exception: Exception => With.logger.quietlyOnException(exception) }
  }

  def prepareEmptySimFile(): Unit = {
    try {
      ensureDirs()
      val hash = With.game.mapHash()
      val f = CombatVisThreads.opponentFile
      val content = new StringBuilder()
      content.append("PW2\n").append("H|").append(hash).append('\n').append("Z\n")
      val fos = new FileOutputStream(f, false)
      try { fos.write(content.toString().getBytes(StandardCharsets.UTF_8)) } finally { try fos.close() catch { case _: Throwable => } }
    } catch { case exception: Exception => With.logger.quietlyOnException(exception) }
  }

  // framesLog: lines of format: frame|id,isFriendly,x,y,alive,width,height;
  // Prepared variant that avoids With.* access on IO thread
}
