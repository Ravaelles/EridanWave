package Debugging

import java.awt.Color
import java.awt.image.BufferedImage

/** Split from EridanSimViz: one role per file. */
object SimVizModel {

  case class UnitMeta(id: Int, friendly: Boolean, width: Int, height: Int, unitType: String, fly: Boolean = false)

  case class UnitFrame(id: Int, friendly: Boolean, x: Int, y: Int, alive: Boolean, width: Int, height: Int, hp: Int = -1, sh: Int = -1, hpMax: Int = -1, shMax: Int = -1, tgt: Option[Int] = None, fly: Boolean = false, cd: Int = -1)

  case class FrameData(f: Int, units: Vector[UnitFrame], attacks: Vector[(Int, Int)] = Vector.empty, deaths: Vector[Int] = Vector.empty)

  final class MapData(val tileW: Int, val tileH: Int, val walkW: Int, val walkH: Int,
                      val tileHeights: Array[Byte], val walkable: Array[Byte]) {
    val pixelW: Int = tileW * 32
    val pixelH: Int = tileH * 32

    // Pre-render to image for fast painting
    lazy val image: BufferedImage = {
      val img = new BufferedImage(pixelW, pixelH, BufferedImage.TYPE_INT_RGB)
      val g = img.getGraphics
      try {
        // Render tiles as 32x32 blocks with grayscale from height
        var ty = 0
        while (ty < tileH) {
          var tx = 0
          while (tx < tileW) {
            val idx = ty * tileW + tx
            val h = (tileHeights(idx) & 0xFF)
            val gray = Math.min(255, 40 + h * 10) // heuristic scaling
            g.setColor(new Color(gray, gray, gray))
            g.fillRect(tx * 32, ty * 32, 32, 32)
            tx += 1
          }
          ty += 1
        }
        // Darken unwalkable mini-tiles
        var wy = 0
        while (wy < walkH) {
          var wx = 0
          while (wx < walkW) {
            val widx = wy * walkW + wx
            val walk = (walkable(widx) & 0xFF) != 0
            if (!walk) {
              val px = (wx * 8)
              val py = (wy * 8)
              g.setColor(new Color(0, 0, 0))
              g.fillRect(px, py, 8, 8)
            }
            wx += 1
          }
          wy += 1
        }
      } finally {
        g.dispose()
      }
      img
    }
  }

  case class SimPack(mapHash: String, metas: Vector[UnitMeta], frames: Vector[FrameData], startGameFrame: Int)
}
