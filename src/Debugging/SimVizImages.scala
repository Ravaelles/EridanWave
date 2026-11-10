package Debugging

import java.awt._
import java.awt.image.BufferedImage
import java.io.File

/** Split from EridanSimViz: one role per file. */
object SimVizImages {

  private val imageCache = new java.util.concurrent.ConcurrentHashMap[String, java.awt.Image]()
  private val missingCache = new java.util.concurrent.ConcurrentHashMap[String, java.lang.Boolean]()
  // Cache for recolored (team-tinted) images: key = name + "|F" or "|E"
  private val recolorCache = new java.util.concurrent.ConcurrentHashMap[String, java.awt.Image]()
  private def candidateImageDirs(): Vector[File] = {
    val dirs = new scala.collection.mutable.ArrayBuffer[File]()

    def addIfValid(base: File): Unit = {
      if (base != null) {
        var cur: File = base.getAbsoluteFile
        var depth = 0
        while (cur != null && depth < 8) {
          val f = new File(cur, "images" + File.separator + "units")
          if (f.exists() && f.isDirectory) dirs += f
          cur = cur.getParentFile
          depth += 1
        }
      }
    }

    // Try from simulation file directory, current working dir, and user.dir
    addIfValid(EridanSimViz.currentSimDir)
    addIfValid(new File("."))
    addIfValid(new File(System.getProperty("user.dir", ".")))

    // Also try alongside the code location (jar/class directory)
    try {
      val url = EridanSimViz.getClass.getProtectionDomain.getCodeSource.getLocation
      if (url != null) addIfValid(new File(url.toURI))
    } catch { case _: Throwable => }

    dirs.distinct.toVector
  }
  private def getUnitImage(name: String): Option[java.awt.Image] = {
    if (name == null || name.isEmpty) return None
    // Try several filename variants to match repository naming conventions
    val variants = Vector(
      name,
      name.replace(' ', '_'),
      name.replace('_', ' ')
    ).distinct
    // Return cached if any variant cached
    variants.foreach { v =>
      val c = imageCache.get(v)
      if (c != null) return Some(c)
    }
    // If any variant is known missing, still try others
    val dirs = candidateImageDirs()
    var img: java.awt.Image = null
    var cachedKey: String = null
    var i = 0
    while (i < dirs.length && img == null) {
      var vi = 0
      while (vi < variants.length && img == null) {
        val key = variants(vi)
        val f = new File(dirs(i), key + ".png")
        if (f.exists()) {
          try {
            img = javax.imageio.ImageIO.read(f)
            cachedKey = key
          } catch { case _: Throwable => img = null }
        }
        vi += 1
      }
      i += 1
    }
    if (img != null) {
      imageCache.put(cachedKey, img)
      Some(img)
    } else {
      variants.foreach(v => missingCache.put(v, true))
      None
    }
  }

  // Return a team-recolored version of the unit image where saturated pink pixels are remapped
  // to blue (friendly) or red (enemy), preserving brightness. Cached per (name, team).
  private[Debugging] def getRecoloredUnitImage(name: String, friendly: Boolean): Option[java.awt.Image] = {
    if (name == null || name.isEmpty) return None

    val cacheKey = name + (if (friendly) "|F" else "|E")
    val cached = recolorCache.get(cacheKey)
    if (cached != null) return Some(cached)

    val baseOpt = getUnitImage(name)
    baseOpt.flatMap { base =>
      try {
        val bw = base.getWidth(null)
        val bh = base.getHeight(null)
        val src = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB)
        val g = src.getGraphics
        try { g.drawImage(base, 0, 0, null) } finally { g.dispose() }
        val out = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB)
        var y = 0
        while (y < bh) {
          var x = 0
          while (x < bw) {
            val argb = src.getRGB(x, y)
            val a = (argb >>> 24) & 0xFF
            val r = (argb >>> 16) & 0xFF
            val gch = (argb >>> 8) & 0xFF
            val b = argb & 0xFF
            if (a == 0) {
              out.setRGB(x, y, argb)
            } else if (isTeamPink(r, gch, b)) {
              // Use the original brightness to set the chosen channel
              val intensity = Math.max(r, b)
              val nr = if (friendly) 0 else intensity
              val ng = 0
              val nb = if (friendly) intensity else 0
              val newArgb = (a << 24) | (nr << 16) | (ng << 8) | nb
              out.setRGB(x, y, newArgb)
            } else {
              out.setRGB(x, y, argb)
            }
            x += 1
          }
          y += 1
        }
        recolorCache.put(cacheKey, out)
        Some(out)
      } catch { case _: Throwable => None }
    }
  }

  // Heuristic to detect saturated pink placeholder pixels in art
  private def isTeamPink(r: Int, g: Int, b: Int): Boolean = {
    // Very saturated pink/magenta: R and B high, G low; allow varying brightness
    val max = Math.max(r, Math.max(g, b))
    val min = Math.min(r, Math.min(g, b))
    val sat = if (max == 0) 0.0 else (max - min).toDouble / max.toDouble
    val brightEnough = max >= 60
    val rbHigh = r >= 180 && b >= 180
    val glow = g <= 80
    (rbHigh && glow) || (brightEnough && sat >= 0.6 && r > g + 60 && b > g + 60)
  }
}
