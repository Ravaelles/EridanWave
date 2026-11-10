package Debugging

import SimVizModel._

import javax.swing._
import java.awt._

/** Split from EridanSimViz: one role per file. */
object SimVizView {

  private[Debugging] class ViewPanel(var map: Option[MapData], var frames: Vector[FrameData]) extends JPanel {
    setPreferredSize(new Dimension(map.map(_.pixelW).getOrElse(800), map.map(_.pixelH).getOrElse(600)))
    setDoubleBuffered(true)

    // Playback state
    @volatile var frameIndex = 0

    // Metas and cooldown maxima (set from main on load/reload)
    @volatile var metas: Vector[UnitMeta] = Vector.empty
    @volatile var cdMaxById: Map[Int, Int] = Map.empty

    // Fallback world size when map is not available
    @volatile var worldW: Int = 0
    @volatile var worldH: Int = 0

    // Auto-zoom state
    @volatile var autoZoomEnabled: Boolean = false
    // zoomRect: (x, y, w, h) in world pixels (pre-zoom, including margins)
    @volatile var zoomRect: Option[(Int, Int, Int, Int)] = None

    private val margin: Int = 64 // pixels (pre-zoom)

    def currentFrameData: Option[FrameData] = if (frames.isEmpty) None else Some(frames(Math.max(0, Math.min(frameIndex, frames.length - 1))))

    override def paintComponent(g: Graphics): Unit = {
      super.paintComponent(g)
      val g2 = g.asInstanceOf[java.awt.Graphics2D]
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

      val w = getWidth
      val h = getHeight

      // Determine base world rect (either full world or auto-zoom bounds), then adjust to panel aspect to fill area
      val (baseX, baseY, worldWidth, worldHeight) = {
        val fullW = map.map(_.pixelW).getOrElse(if (worldW > 0) worldW else 1024)
        val fullH = map.map(_.pixelH).getOrElse(if (worldH > 0) worldH else 768)
        val init = if (autoZoomEnabled) zoomRect.getOrElse((0, 0, fullW, fullH)) else (0, 0, fullW, fullH)
        var ax = init._1; var ay = init._2; var aw = Math.max(1, init._3); var ah = Math.max(1, init._4)
        val panelAR = if (h > 0) w.toDouble / h.toDouble else 1.0
        val rectAR = aw.toDouble / ah.toDouble
        if (Math.abs(rectAR - panelAR) > 1e-6) {
          if (rectAR < panelAR) {
            // Too tall: need to expand width
            val desiredW = Math.max(1, (ah * panelAR).toInt)
            val delta = desiredW - aw
            ax -= delta / 2
            aw = desiredW
          } else {
            // Too wide: need to expand height
            val desiredH = Math.max(1, (aw / panelAR).toInt)
            val delta = desiredH - ah
            ay -= delta / 2
            ah = desiredH
          }
          // Clamp to map/world bounds
          if (ax < 0) ax = 0
          if (ay < 0) ay = 0
          if (ax + aw > fullW) ax = Math.max(0, fullW - aw)
          if (ay + ah > fullH) ay = Math.max(0, fullH - ah)
        }
        (ax, ay, Math.max(1, Math.min(aw, fullW)), Math.max(1, Math.min(ah, fullH)))
      }

      val scale = Math.min(w.toDouble / worldWidth, h.toDouble / worldHeight)
      val drawW = Math.max(1, (worldWidth * scale).toInt)
      val drawH = Math.max(1, (worldHeight * scale).toInt)
      val offX = (w - drawW) / 2
      val offY = (h - drawH) / 2

      // Draw terrain or fallback background
      map match {
        case Some(m) =>
          if (autoZoomEnabled) {
            // Clamp source rect to map bounds
            val sx1 = Math.max(0, baseX)
            val sy1 = Math.max(0, baseY)
            val sx2 = Math.min(m.pixelW, baseX + worldWidth)
            val sy2 = Math.min(m.pixelH, baseY + worldHeight)
            g2.drawImage(m.image, offX, offY, offX + drawW, offY + drawH, sx1, sy1, sx2, sy2, null)
          } else {
            g2.drawImage(m.image, offX, offY, drawW, drawH, null)
          }
        case None =>
          g2.setColor(new Color(32, 32, 32))
          g2.fillRect(offX, offY, drawW, drawH)
      }

      // Draw units and events for current frame
      currentFrameData.foreach { fd =>
        // Build a lookup map for positions by id
        val posById = fd.units.map(u => u.id -> u).toMap
        val blue = new Color(0, 0, 255)
        val red = new Color(255, 0, 0)
        val blueDark = blue.darker()
        val redDark = red.darker()
        val pink = new Color(255, 105, 180)
        // Draw target lines (darker team color)
        fd.units.foreach { u =>
          u.tgt.foreach { tid =>
            posById.get(tid).foreach { tv =>
              if (u.alive) {
                val sx = (offX + (u.x - baseX) * scale).toInt
                val sy = (offY + (u.y - baseY) * scale).toInt
                val tx = (offX + (tv.x - baseX) * scale).toInt
                val ty = (offY + (tv.y - baseY) * scale).toInt
                g2.setColor(if (u.friendly) new Color(blueDark.getRed, blueDark.getGreen, blueDark.getBlue, 180) else new Color(redDark.getRed, redDark.getGreen, redDark.getBlue, 180))
                g2.drawLine(sx, sy, tx, ty)
              }
            }
          }
        }
        // First draw units with health fill and outline
        val metaById: Map[Int, UnitMeta] = metas.map(m => m.id -> m).toMap
        fd.units.foreach { u =>
          if (u.alive) {
            val color = if (u.friendly) blue else red
            // Health fraction
            val maxTotal = Math.max(0, u.hpMax) + Math.max(0, u.shMax)
            val curTotal = Math.max(0, u.hp) + Math.max(0, u.sh)
            val frac = if (maxTotal > 0) Math.max(0.0, Math.min(1.0, curTotal.toDouble / maxTotal)) else 1.0
            if (u.fly) {
              // Draw circle with clipped health fill
              val cx = offX + (u.x - baseX) * scale
              val cy = offY + (u.y - baseY) * scale
              val diam = Math.max(u.width, u.height) * scale
              val r = diam / 2.0
              val ellipse = new java.awt.geom.Ellipse2D.Double(cx - r, cy - r, diam, diam)
              val oldClip = g2.getClip
              g2.setClip(ellipse)
              g2.setColor(if (u.friendly) new Color(0, 0, 255, 140) else new Color(255, 0, 0, 140))
              val filled = Math.max(1, (diam * frac).toInt)
              g2.fillRect((cx - r).toInt, (cy + r - filled).toInt, Math.max(1, diam.toInt), filled)
              g2.setClip(oldClip)
              g2.setColor(color)
              g2.draw(ellipse)
              // Draw unit image centered over circle, at intrinsic image size scaled by global scale
              metaById.get(u.id).flatMap(m => SimVizImages.getRecoloredUnitImage(m.unitType, u.friendly)).foreach { img =>
                val iw = Math.max(1, (img.getWidth(null).toDouble  * scale).toInt)
                val ih = Math.max(1, (img.getHeight(null).toDouble * scale).toInt)
                val ix = (cx - iw / 2.0).toInt
                val iy = (cy - ih / 2.0).toInt
                g2.drawImage(img, ix, iy, iw, ih, null)
              }
              // Cooldown bar over image at bottom of circle bounds
              val cdMax = cdMaxById.getOrElse(u.id, Math.max(1, u.cd))
              if (u.cd >= 0 && cdMax > 0) {
                val fracCd = Math.max(0.0, Math.min(1.0, u.cd.toDouble / cdMax))
                val barH = Math.max(2, (2 * scale).toInt)
                val barY = (cy + r - barH).toInt
                val barW = Math.max(1, (diam * fracCd).toInt)
                g2.setColor(new Color(255, 215, 0, 220))
                g2.fillRect((cx - r).toInt, barY, barW, barH)
              }
            } else {
              val left = u.x - u.width / 2
              val top  = u.y - u.height / 2
              val sx = (offX + (left - baseX) * scale).toInt
              val sy = (offY + (top  - baseY) * scale).toInt
              val sw = Math.max(1, (u.width  * scale).toInt)
              val sh = Math.max(1, (u.height * scale).toInt)
              val filled = Math.max(1, (sh * frac).toInt)
              // Fill bottom portion
              g2.setColor(if (u.friendly) new Color(0, 0, 255, 140) else new Color(255, 0, 0, 140))
              g2.fillRect(sx, sy + (sh - filled), sw, filled)
              // Outline
              g2.setColor(color)
              g2.drawRect(sx, sy, sw, sh)
              // Draw unit image at intrinsic size scaled by global scale, centered on unit position
              metaById.get(u.id).flatMap(m => SimVizImages.getRecoloredUnitImage(m.unitType, u.friendly)).foreach { img =>
                val cx = offX + (u.x - baseX) * scale
                val cy = offY + (u.y - baseY) * scale
                val iw = Math.max(1, (img.getWidth(null).toDouble  * scale).toInt)
                val ih = Math.max(1, (img.getHeight(null).toDouble * scale).toInt)
                val ix = (cx - iw / 2.0).toInt
                val iy = (cy - ih / 2.0).toInt
                g2.drawImage(img, ix, iy, iw, ih, null)
              }
              // Cooldown bar at bottom of box
              val cdMax = cdMaxById.getOrElse(u.id, Math.max(1, u.cd))
              if (u.cd >= 0 && cdMax > 0) {
                val fracCd = Math.max(0.0, Math.min(1.0, u.cd.toDouble / cdMax))
                val barH = Math.max(2, (2 * scale).toInt)
                val barY = sy + sh - barH
                val barW = Math.max(1, (sw * fracCd).toInt)
                g2.setColor(new Color(255, 215, 0, 220))
                g2.fillRect(sx, barY, barW, barH)
              }
            }
          }
        }
        // Draw death indicators (solid red) for deaths in the last 8 frames
        val recentIdxs = {
          val buf = new scala.collection.mutable.ArrayBuffer[Int](8)
          var k = 0
          while (k < 8) { val idx = frameIndex - k; if (idx >= 0 && idx < frames.length) buf += idx; k += 1 }
          buf.toVector
        }
        if (recentIdxs.nonEmpty) {
          g2.setColor(Color.RED)
          recentIdxs.foreach { idx =>
            val fdr = frames(idx)
            val pos = fdr.units.map(u => u.id -> u).toMap
            fdr.deaths.foreach { id =>
              pos.get(id).foreach { u =>
                if (u.fly) {
                  val cx = offX + (u.x - baseX) * scale
                  val cy = offY + (u.y - baseY) * scale
                  val diam = Math.max(u.width, u.height) * scale
                  val r = diam / 2.0
                  val ellipse = new java.awt.geom.Ellipse2D.Double(cx - r, cy - r, diam, diam)
                  g2.fill(ellipse)
                } else {
                  val left = u.x - u.width / 2
                  val top  = u.y - u.height / 2
                  val sx = (offX + (left - baseX) * scale).toInt
                  val sy = (offY + (top  - baseY) * scale).toInt
                  val sw = Math.max(1, (u.width  * scale).toInt)
                  val sh = Math.max(1, (u.height * scale).toInt)
                  g2.fillRect(sx - 1, sy - 1, sw + 2, sh + 2)
                }
              }
            }
          }
        }
        // Flash victims white for the same recent window as attack lines, then draw attack lines on top
        if (recentIdxs.nonEmpty) {
          // Compute set of recent victims
          val victims = new java.util.HashSet[Int]()
          var vi = 0
          while (vi < recentIdxs.length) {
            val fdr = frames(recentIdxs(vi))
            var ak = 0
            while (ak < fdr.attacks.length) { victims.add(fdr.attacks(ak)._2); ak += 1 }
            vi += 1
          }
          if (!victims.isEmpty) {
            // Build a lookup for most recent positions to place the flash
            val latest = frames(recentIdxs.head).units.map(u => u.id -> u).toMap
            val it = victims.iterator()
            while (it.hasNext) {
              val id = it.next()
              latest.get(id).foreach { u =>
                // Flash the sprite image, not the HP background
                metas.find(_.id == u.id).flatMap(m => SimVizImages.getRecoloredUnitImage(m.unitType, u.friendly)).foreach { img =>
                  val cx = offX + (u.x - baseX) * scale
                  val cy = offY + (u.y - baseY) * scale
                  val iw = Math.max(1, (img.getWidth(null).toDouble  * scale).toInt)
                  val ih = Math.max(1, (img.getHeight(null).toDouble * scale).toInt)
                  val ix = (cx - iw / 2.0).toInt
                  val iy = (cy - ih / 2.0).toInt
                  val oldComp = g2.getComposite
                  g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f))
                  g2.setColor(Color.WHITE)
                  g2.fillRect(ix, iy, iw, ih)
                  g2.setComposite(oldComp)
                }
              }
            }
          }
          // Draw attack lines in attacker bright color, persisted for 3 frames and on top with thicker stroke
          val oldStroke = g2.getStroke
          g2.setStroke(new BasicStroke(3f))
          recentIdxs.foreach { idx =>
            val fdr = frames(idx)
            val pos = fdr.units.map(u => u.id -> u).toMap
            fdr.attacks.foreach { case (a, v) =>
              (pos.get(a), pos.get(v)) match {
                case (Some(ua), Some(uv)) =>
                  val ax = (offX + (ua.x - baseX) * scale).toInt
                  val ay = (offY + (ua.y - baseY) * scale).toInt
                  val vx = (offX + (uv.x - baseX) * scale).toInt
                  val vy = (offY + (uv.y - baseY) * scale).toInt
                  val ac = if (ua.friendly) new Color(0, 0, 255, 220) else new Color(255, 0, 0, 220)
                  g2.setColor(ac)
                  g2.drawLine(ax, ay, vx, vy)
                case _ =>
              }
            }
          }
          g2.setStroke(oldStroke)
        }
      }
    }
  }

  // Sidebar panel showing a tally of unit deaths per side up to the current frame
  private[Debugging] class DeathTallyPanel(
    metasRef: java.util.concurrent.atomic.AtomicReference[Vector[UnitMeta]],
    framesRef: java.util.concurrent.atomic.AtomicReference[Vector[FrameData]],
    currentIndex: () => Int
  ) extends JPanel {
    setPreferredSize(new Dimension(140, 200))
    setOpaque(true)
    setBackground(new Color(24, 24, 24))
    override def paintComponent(g: Graphics): Unit = {
      super.paintComponent(g)
      val g2 = g.asInstanceOf[java.awt.Graphics2D]
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
      val blue = new Color(0, 0, 255)
      val red = new Color(255, 0, 0)
      val frames = framesRef.get()
      val idx = Math.max(0, Math.min(currentIndex(), Math.max(0, frames.length - 1)))
      val metas = metasRef.get().map(m => m.id -> m).toMap
      val deadF = new scala.collection.mutable.ArrayBuffer[Int]()
      val deadE = new scala.collection.mutable.ArrayBuffer[Int]()
      var i = 0
      while (i <= idx && i < frames.length) {
        val f = frames(i)
        var j = 0
        while (j < f.deaths.length) {
          val id = f.deaths(j)
          metas.get(id).foreach { m =>
            if (m.friendly) deadF += id else deadE += id
          }
          j += 1
        }
        i += 1
      }
      val uniqF = deadF.distinct
      val uniqE = deadE.distinct
      val w = getWidth
      val pad = 10
      val colW = (w - pad * 3) / 2
      val box = 10
      g2.setColor(Color.WHITE)
      g2.drawString("Deaths", pad, 14)
      // Totals just below header
      g2.drawString(s"F: ${uniqF.size}", pad, 26)
      g2.drawString(s"E: ${uniqE.size}", pad * 2 + colW, 26)
      // Columns from top with portraits
      val maxThumb = Math.max(12, Math.min(48, colW - 8))
      var y = 42
      // Friendly (left column)
      uniqF.foreach { id =>
        val xCol = pad
        metas.get(id).flatMap(m => SimVizImages.getRecoloredUnitImage(m.unitType, friendly = true)) match {
          case Some(img) =>
            val iw = Math.max(1, img.getWidth(null))
            val ih = Math.max(1, img.getHeight(null))
            val scale = Math.min(maxThumb.toDouble / iw.toDouble, maxThumb.toDouble / ih.toDouble)
            val dw = Math.max(1, (iw * scale).toInt)
            val dh = Math.max(1, (ih * scale).toInt)
            val dx = xCol + (colW - dw) / 2
            val dy = y
            // Border
            g2.setColor(blue)
            g2.drawRect(dx - 1, dy - 1, dw + 2, dh + 2)
            // Image
            g2.drawImage(img, dx, dy, dw, dh, null)
            y += dh + 6
          case None =>
            g2.setColor(blue)
            g2.fillRect(xCol + (colW - box) / 2, y, box, box)
            y += box + 6
        }
      }
      // Enemy (right column)
      y = 28
      uniqE.foreach { id =>
        val xCol = pad * 2 + colW
        metas.get(id).flatMap(m => SimVizImages.getRecoloredUnitImage(m.unitType, friendly = false)) match {
          case Some(img) =>
            val iw = Math.max(1, img.getWidth(null))
            val ih = Math.max(1, img.getHeight(null))
            val scale = Math.min(maxThumb.toDouble / iw.toDouble, maxThumb.toDouble / ih.toDouble)
            val dw = Math.max(1, (iw * scale).toInt)
            val dh = Math.max(1, (ih * scale).toInt)
            val dx = xCol + (colW - dw) / 2
            val dy = y
            g2.setColor(red)
            g2.drawRect(dx - 1, dy - 1, dw + 2, dh + 2)
            g2.drawImage(img, dx, dy, dw, dh, null)
            y += dh + 6
          case None =>
            g2.setColor(red)
            g2.fillRect(xCol + (colW - box) / 2, y, box, box)
            y += box + 6
        }
      }
    }
  }
}
