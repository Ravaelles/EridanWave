package Debugging

import SimVizModel._

import mjson.Json

import java.awt._
import java.io.{DataInputStream, File, FileInputStream, RandomAccessFile}
import java.util.zip.GZIPInputStream
import javax.swing._
import scala.util.Try

/**
  * Combat simulation viewer with scaling, playback controls, and auto-reload toggle.
  * Usage: Debugging.CombatVisualizer <path-to-simulation.pwcs>
  * If no path is provided, defaults to simulation.pwcs in the working directory (falls back to simulation.json).
  */
object EridanSimViz {

  // External diagnostics to c:\p\pw\out\logs for robust investigation
  private def extVisLog(message: String): Unit = {
    try {
      val dir = new java.io.File("C:\\p\\pw\\out\\logs")
      if (!dir.exists()) dir.mkdirs()
      val ts = System.currentTimeMillis()
      val sdf = new java.text.SimpleDateFormat("HH:mm:ss.SSS")
      val line = s"[$ts ${sdf.format(new java.util.Date(ts))}] $message\n"
      val f = new java.io.File(dir, "combatvis.log")
      val fos = new java.io.FileOutputStream(f, true)
      try fos.write(line.getBytes(java.nio.charset.StandardCharsets.UTF_8)) finally try fos.close() catch { case _: Throwable => }
    } catch { case _: Throwable => }
  }

  // Stderr logger for immediate user diagnostics
  private def err(message: String): Unit = {
    try {
      System.err.println("[EridanSimViz] " + message)
    } catch { case _: Throwable => () }
  }

  @volatile private var window: JFrame = _
  @volatile private var timerRef: javax.swing.Timer = _
  // Directory of the current simulation file (used to find images)
  @volatile private[Debugging] var currentSimDir: File = new File(".")
  def requestClose(): Unit = {
    try {
      SwingUtilities.invokeLater(new Runnable { override def run(): Unit = {
        try { if (timerRef != null) timerRef.stop() } catch { case _: Throwable => }
        try { if (window != null) window.dispose() } catch { case _: Throwable => }
      }})
    } catch { case _: Throwable => }
  }

  private def loadMap(mapDir: File, mapHash: String): Option[MapData] = {
    val f = new File(mapDir, s"$mapHash.mapbin")
    if (!f.exists()) return None

    Try {
      val dis = new DataInputStream(new FileInputStream(f))
      try {
        val tileW = dis.readInt()
        val tileH = dis.readInt()
        val walkW = dis.readInt()
        val walkH = dis.readInt()
        val tileCount = tileW * tileH
        val walkCount = walkW * walkH
        val heights = new Array[Byte](tileCount)
        dis.readFully(heights)
        val walkable = new Array[Byte](walkCount)
        dis.readFully(walkable)
        new MapData(tileW, tileH, walkW, walkH, heights, walkable)
      } finally dis.close()
    }.toOption
  }

  // Safe JSON accessors (mjson can return nulls; guard them carefully)

  def main(args: Array[String]): Unit = {
    // Determine simulation file path (.pwcs preferred, fallback to .json)
    // Sanitize Launch4j-injected literal "%1" and pick a usable path argument if provided
    val cleanedArgs: Vector[String] =
      Option(args).map(_.toVector).getOrElse(Vector.empty)
        .filter(s => s != null && s.trim.nonEmpty)
        .filterNot(_ == "%1")
    // Prefer the first argument that points to an existing file; otherwise use the first remaining
    val chosenArgOpt: Option[String] =
      cleanedArgs.find(p => try new File(p).exists() catch { case _: Throwable => false }).orElse(cleanedArgs.headOption)
    val argPathOpt = chosenArgOpt
    val simFile: File = {
      val candidate = argPathOpt.map(new File(_)).getOrElse(new File("simulation.pwcs"))
      val f =
        if (candidate.isDirectory) {
          val pwcs = new File(candidate, "simulation.pwcs")
          if (pwcs.exists()) pwcs else new File(candidate, "simulation.json")
        } else if (candidate.exists()) candidate
        else {
          val pwcs = new File("simulation.pwcs")
          if (pwcs.exists()) pwcs else new File("simulation.json")
        }
      f
    }
    val argStr = Option(args).map(_.mkString(" ")).getOrElse("<none>")
    err("Startup args: " + argStr)
    err("Resolved sim file: " + simFile.getAbsolutePath + s" exists=${simFile.exists()} len=${if (simFile.exists()) simFile.length() else -1} lm=${simFile.lastModified()}")
    try { extVisLog(s"Startup args='$argStr' file='${simFile.getAbsolutePath}' exists=${simFile.exists()} len=${if (simFile.exists()) simFile.length() else -1}") } catch { case _: Throwable => }
    val baseDir = simFile.getParentFile
    currentSimDir = baseDir
    val mapDir = baseDir

    // State shared with UI thread
    val lastModifiedRef = new java.util.concurrent.atomic.AtomicLong(0L)
    val autoReloadRef = new java.util.concurrent.atomic.AtomicBoolean(true)
    val mapRef = new java.util.concurrent.atomic.AtomicReference[Option[MapData]](None)
    val framesRef = new java.util.concurrent.atomic.AtomicReference[Vector[FrameData]](Vector.empty)
    val metasRef = new java.util.concurrent.atomic.AtomicReference[Vector[UnitMeta]](Vector.empty)
    val simsRef = new java.util.concurrent.atomic.AtomicReference[Vector[SimPack]](Vector.empty)
    // Incremental reading state
    val fileLenRef = new java.util.concurrent.atomic.AtomicLong(0L)
    val pendingRef = new java.util.concurrent.atomic.AtomicReference[String]("")

    def readAllText(f: File): String = {
      var fis: java.io.FileInputStream = null
      var bis: java.io.BufferedInputStream = null
      try {
        fis = new FileInputStream(f)
        bis = new java.io.BufferedInputStream(fis)
        bis.mark(4)
        val b1 = bis.read(); val b2 = bis.read(); bis.reset()
        val isGz = (b1 == 0x1f && b2 == 0x8b)
        val in: java.io.InputStream = if (isGz) new GZIPInputStream(bis) else bis
        try scala.io.Source.fromInputStream(in)(scala.io.Codec.UTF8).mkString finally in.close()
      } finally {
        try if (bis != null) bis.close() catch { case _: Throwable => }
        try if (fis != null) fis.close() catch { case _: Throwable => }
      }
    }

    def splitPW2Blocks(txt: String): Vector[String] = {
      val lines = txt.split("\r?\n")
      val out = new scala.collection.mutable.ArrayBuffer[String]()
      var i = 0
      var cur = new StringBuilder()
      var in = false
      while (i < lines.length) {
        val line = lines(i)
        val lt = if (line == null) "" else line.trim
        if (lt == "PW2") {
          if (in) { cur = new StringBuilder() }
          in = true
          cur.append("PW2\n")
        } else if (in) {
          cur.append(line).append('\n')
          if (lt == "Z") { out += cur.toString(); cur = new StringBuilder(); in = false }
        }
        i += 1
      }
      out.toVector
    }

    // Streaming splitter: returns (completeBlocks, remainder)
    def splitPW2BlocksWithRemainder(txt: String): (Vector[String], String) = {
      val lines = txt.split("\r?\n", -1) // keep trailing empty
      val out = new scala.collection.mutable.ArrayBuffer[String]()
      val cur = new StringBuilder()
      var in = false
      var i = 0
      while (i < lines.length) {
        val line = lines(i)
        val lt = if (line == null) "" else line.trim
        if (lt == "PW2") {
          if (in) {
            // Unexpected new PW2 without Z; discard previous partial and start fresh
            cur.clear()
          }
          in = true
          cur.append("PW2\n")
        } else if (in) {
          cur.append(line).append('\n')
          if (lt == "Z") {
            out += cur.toString()
            cur.clear()
            in = false
          }
        }
        i += 1
      }
      val rem = if (in && cur.length > 0) cur.toString() else ""
      (out.toVector, rem)
    }

    def readAppended(file: File): (String, Long, Boolean) = {
      var raf: RandomAccessFile = null
      try {
        raf = new RandomAccessFile(file, "r")
        val curLen = fileLenRef.get()
        val fileLen = raf.length()
        if (fileLen < curLen) {
          // Truncated (new game) – reset
          fileLenRef.set(0L)
          raf.seek(0L)
          val bytes = new Array[Byte](fileLen.toInt)
          raf.readFully(bytes)
          (new String(bytes, java.nio.charset.StandardCharsets.UTF_8), fileLen, true)
        } else if (fileLen > curLen) {
          raf.seek(curLen)
          val toRead = (fileLen - curLen).toInt
          val bytes = new Array[Byte](toRead)
          raf.readFully(bytes)
          (new String(bytes, java.nio.charset.StandardCharsets.UTF_8), fileLen, false)
        } else {
          ("", fileLen, false)
        }
      } catch { case _: Throwable => ("", fileLenRef.get(), false) }
      finally { try if (raf != null) raf.close() catch { case _: Throwable => } }
    }

    def parseAllSims(file: File): Vector[SimPack] = {
      if (!file.exists()) { err("parseAllSims: file does not exist: " + file.getAbsolutePath); return Vector.empty }
      // Stream the file and keep only the last N complete PW2 blocks to bound memory
      val MaxSimsToKeep = 200
      var fis: java.io.FileInputStream = null
      var bis: java.io.BufferedInputStream = null
      try {
        fis = new FileInputStream(file)
        bis = new java.io.BufferedInputStream(fis, 1 << 16)
        val buf = new Array[Byte](1 << 15) // 32 KB chunks
        val deque = new java.util.ArrayDeque[String]()
        var pending = ""
        var read = 0
        var totalRead = 0L
        while ({ read = bis.read(buf); read } != -1) {
          totalRead += read
          val seg = new String(buf, 0, read, java.nio.charset.StandardCharsets.UTF_8)
          val (blocks, rem) = splitPW2BlocksWithRemainder(pending + seg)
          pending = rem
          var i = 0
          while (i < blocks.length) {
            if (deque.size() >= MaxSimsToKeep) deque.pollFirst()
            deque.addLast(blocks(i))
            i += 1
          }
        }
        // If we saw no PW2 blocks at all, try legacy JSON by reading the entire (likely small) file
        if (deque.isEmpty) {
          err(s"parseAllSims: no PW2 blocks found in streaming read (bytes=$totalRead); trying full read")
          var txt = readAllText(file)
          if (txt != null && txt.nonEmpty && txt.charAt(0) == '\uFEFF') {
            // Strip UTF-8 BOM if present
            txt = txt.substring(1)
          }
          val head = if (txt.length > 64) txt.substring(0, 64).replace('\n', ' ') else txt
          err("parseAllSims: full-read head='" + head + "'")
          val parseFromText: Vector[SimPack] = {
            if (txt.startsWith("PW2")) {
              val blocks = splitPW2Blocks(txt)
              err("parseAllSims: PW2 full-read blocks=" + blocks.length)
              blocks.flatMap { b => Try(SimVizParse.parsePW2(b)).toOption }.sortBy(s => -s.startGameFrame)
            } else {
              // Try regex-based extraction tolerant to extra whitespace lines and mixed endings
              try {
                val pattern = java.util.regex.Pattern.compile("(?ms)^\\s*PW2\\s*$.*?^\\s*Z\\s*$")
                val m = pattern.matcher(txt)
                val buf = new scala.collection.mutable.ArrayBuffer[String]()
                while (m.find()) { buf += m.group() }
                if (buf.nonEmpty) {
                  err("parseAllSims: regex PW2 blocks found=" + buf.length)
                  buf.flatMap(b => Try(SimVizParse.parsePW2(b)).toOption).toVector.sortBy(s => -s.startGameFrame)
                } else {
                  err("parseAllSims: non-PW2; attempting legacy JSON parse")
                  Vector(SimVizParse.parseJsonSim(txt))
                }
              } catch { case _: Throwable => Vector(SimVizParse.parseJsonSim(txt)) }
            }
          }
          err("parseAllSims: fallback sims parsed=" + parseFromText.length)
          parseFromText
        } else {
          // Parse the kept blocks to SimPacks and sort newest first
          err("parseAllSims: streaming PW2 blocks found=" + deque.size())
          val sims = new scala.collection.mutable.ArrayBuffer[SimPack](deque.size())
          val it = deque.iterator()
          while (it.hasNext) {
            val blk = it.next()
            Try(SimVizParse.parsePW2(blk)).foreach(sims += _)
          }
          val out = sims.toVector.sortBy(s => -s.startGameFrame)
          err("parseAllSims: sims parsed=" + out.length)
          out
        }
      } catch { case t: Throwable => err("parseAllSims exception: " + t.toString); Vector.empty }
      finally {
        try if (bis != null) bis.close() catch { case _: Throwable => }
        try if (fis != null) fis.close() catch { case _: Throwable => }
      }
    }

    // cooldown maxima per unit id
    def computeCdMax(frames: Vector[FrameData]): Map[Int, Int] = {
      val m = new scala.collection.mutable.HashMap[Int, Int]()
      var i = 0
      while (i < frames.length) {
        val f = frames(i)
        var j = 0
        while (j < f.units.length) {
          val u = f.units(j)
          if (u.cd >= 0) m.put(u.id, Math.max(m.getOrElse(u.id, 0), u.cd))
          j += 1
        }
        i += 1
      }
      m.toMap
    }
    val cdMaxRef = new java.util.concurrent.atomic.AtomicReference[Map[Int, Int]](Map.empty)

    // Auto-zoom bounds
    val zoomRef = new java.util.concurrent.atomic.AtomicReference[Option[(Int, Int, Int, Int)]](None)

    // Initial load
    def computeWorldSize(frames: Vector[FrameData]): (Int, Int) = {
      var maxX = 0
      var maxY = 0
      var i = 0
      while (i < frames.length) {
        val f = frames(i)
        var j = 0
        while (j < f.units.length) {
          val u = f.units(j)
          if (u.x > maxX) maxX = u.x
          if (u.y > maxY) maxY = u.y
          j += 1
        }
        i += 1
      }
      (Math.max(1024, maxX + 64), Math.max(768, maxY + 64))
    }
    def computeAutoZoomBounds(frames: Vector[FrameData]): Option[(Int, Int, Int, Int)] = {
      if (frames.isEmpty) return None
      // Collect IDs that deal or receive damage (attackers, victims) and deaths
      val ids = new scala.collection.mutable.HashSet[Int]()
      var i = 0
      while (i < frames.length) {
        val f = frames(i)
        var k = 0
        while (k < f.attacks.length) {
          val pair = f.attacks(k)
          ids += pair._1; ids += pair._2
          k += 1
        }
        var d = 0
        while (d < f.deaths.length) { ids += f.deaths(d); d += 1 }
        i += 1
      }
      if (ids.isEmpty) return None

      var minX = Int.MaxValue
      var minY = Int.MaxValue
      var maxX = Int.MinValue
      var maxY = Int.MinValue
      i = 0
      while (i < frames.length) {
        val f = frames(i)
        var j = 0
        while (j < f.units.length) {
          val u = f.units(j)
          if (ids.contains(u.id)) {
            if (u.x < minX) minX = u.x
            if (u.y < minY) minY = u.y
            if (u.x > maxX) maxX = u.x
            if (u.y > maxY) maxY = u.y
          }
          j += 1
        }
        i += 1
      }
      if (minX == Int.MaxValue) None
      else {
        val margin = 64
        // Best-fit rectangle including margin (no squaring)
        val bx = Math.max(0, minX - margin)
        val by = Math.max(0, minY - margin)
        val bw = Math.max(1, (maxX - minX) + margin * 2)
        val bh = Math.max(1, (maxY - minY) + margin * 2)
        Some((bx, by, bw, bh))
      }
    }
    // Initial parse of all sims and load newest
    var all0 = parseAllSims(simFile)
    // Fallback: if nothing parsed (e.g., gzipped or legacy), try a full read
    if (all0.isEmpty && simFile.exists()) {
      try {
        val txt = readAllText(simFile)
        if (txt != null && txt.nonEmpty) {
          if (txt.startsWith("PW2")) {
            val blocks = splitPW2Blocks(txt)
            all0 = blocks.flatMap(b => Try(SimVizParse.parsePW2(b)).toOption).sortBy(s => -s.startGameFrame)
          } else {
            all0 = Vector(SimVizParse.parseJsonSim(txt))
          }
        }
      } catch { case _: Throwable => () }
    }
    simsRef.set(all0)
    if (all0.nonEmpty) {
      val pack = all0.head
      mapRef.set(loadMap(mapDir, pack.mapHash))
      framesRef.set(pack.frames)
      metasRef.set(pack.metas)
      cdMaxRef.set(computeCdMax(pack.frames))
      zoomRef.set(computeAutoZoomBounds(pack.frames))
    } else {
      try { extVisLog(s"Initial load found no simulations in ${simFile.getAbsolutePath}") } catch { case _: Throwable => }
    }
    lastModifiedRef.set(simFile.lastModified())
    fileLenRef.set(if (simFile.exists()) simFile.length() else 0L)
    pendingRef.set("")

    SwingUtilities.invokeLater(new Runnable { override def run(): Unit = {
      val panel = new SimVizView.ViewPanel(mapRef.get(), framesRef.get())
      panel.metas = metasRef.get()
      panel.cdMaxById = cdMaxRef.get()
      // Initialize fallback world size if map missing
      if (panel.map.isEmpty) {
        val (ww, wh) = computeWorldSize(framesRef.get())
        panel.worldW = ww
        panel.worldH = wh
      }
      // Initialize auto-zoom
      panel.zoomRect = zoomRef.get()
      panel.autoZoomEnabled = true

      // Controls
      val playPause = new JButton("Pause")
      val reloadBtn = new JButton("Reload")
      val autoReload = new JCheckBox("Auto-reload", true)
      val autoZoom = new JCheckBox("Auto Zoom", true)
      val timeLabel = new JLabel("")
      val slider = new JSlider()
      slider.setMinimum(0)
      slider.setMaximum(Math.max(0, Math.max(0, panel.frames.length - 1)))
      slider.setValue(0)
      slider.setPaintTicks(false)
      slider.setPaintLabels(false)

      def updateTimeLabel(): Unit = {
        if (panel.frames.nonEmpty) {
          val minF = panel.frames.head.f
          val maxF = panel.frames.last.f
          val cur  = panel.currentFrameData.map(_.f).getOrElse(0)
          timeLabel.setText(f"Frame $cur%,d  (range $minF%,d .. $maxF%,d)")
        } else timeLabel.setText("No frames")
      }

      // Frame
      val frame = new JFrame("EridanSimViz")
      window = frame
      frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
      frame.setLayout(new BorderLayout())

      // Left simulation list
      val simListModel = new DefaultListModel[String]()
      val simList = new JList[String](simListModel)
      simList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
      val simScroll = new JScrollPane(simList)
      simScroll.setPreferredSize(new Dimension(100, 200))
      frame.getContentPane.add(simScroll, BorderLayout.WEST)

      def labelFor(pack: SimPack): String = {
        val totalSec = Math.max(0, pack.startGameFrame / 24)
        val mm = totalSec / 60
        val ss = totalSec % 60
        val fmod = Math.floorMod(pack.startGameFrame, 24)
        f"$mm%d:$ss%02d +$fmod%d"
      }
      def populateSimList(sims: Vector[SimPack]): Unit = {
        simListModel.clear()
        // Show all simulations, including empty placeholders, so the user always has a selectable tab
        var i = 0
        val labels = new scala.collection.mutable.ArrayBuffer[String]()
        while (i < sims.length) { val lab = labelFor(sims(i)); simListModel.addElement(lab); labels += lab; i += 1 }
        try { extVisLog(s"populateSimList: count=${sims.length} labels=${labels.mkString(", ")}") } catch { case _: Throwable => }
      }
      def loadSimAt(index: Int): Unit = {
        val sims = simsRef.get()
        if (index >= 0 && index < sims.length) {
          val pack = sims(index)
          mapRef.set(loadMap(mapDir, pack.mapHash))
          framesRef.set(pack.frames)
          metasRef.set(pack.metas)
          cdMaxRef.set(computeCdMax(pack.frames))
          zoomRef.set(computeAutoZoomBounds(pack.frames))
          panel.map = mapRef.get()
          panel.frames = pack.frames
          panel.metas = pack.metas
          panel.cdMaxById = cdMaxRef.get()
          panel.zoomRect = zoomRef.get()
          if (panel.map.isEmpty) {
            val (ww, wh) = computeWorldSize(pack.frames)
            panel.worldW = ww; panel.worldH = wh
          }
          slider.setMaximum(Math.max(0, pack.frames.length - 1))
          panel.frameIndex = Math.min(panel.frameIndex, Math.max(0, pack.frames.length - 1))
          slider.setValue(panel.frameIndex)
          panel.revalidate(); panel.repaint(); updateTimeLabel()
        }
      }

      // Death tally panel on the right
      val tally = new SimVizView.DeathTallyPanel(metasRef, framesRef, () => panel.frameIndex)
      frame.getContentPane.add(panel, BorderLayout.CENTER)
      frame.getContentPane.add(tally, BorderLayout.EAST)

      // Populate list initially
      populateSimList(simsRef.get())
      if (simsRef.get().nonEmpty) {
        val simsNow = simsRef.get()
        var idx = 0
        var found = false
        while (idx < simsNow.length && !found) {
          if (simsNow(idx).frames.nonEmpty) found = true else idx += 1
        }
        val sel = if (found) idx else 0
        simList.setSelectedIndex(sel)
        loadSimAt(sel)
      }

      // Disable auto-reload on manual selection and load selected sim
      simList.addListSelectionListener(_ => {
        if (!simList.getValueIsAdjusting) {
          val idx = simList.getSelectedIndex
          if (idx >= 0) {
            autoReloadRef.set(false)
            autoReload.setSelected(false)
            loadSimAt(idx)
          }
        }
      })

      val controls = new JPanel()
      controls.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT))
      controls.add(playPause)
      controls.add(slider)
      // Playback speed slider (1x,2x,3x,4x,6x,8x)
      val speedOptions = Array(1, 2, 3, 4, 6, 8)
      val speedSlider = new JSlider(0, speedOptions.length - 1, 4)
      val speedLabels = new java.util.Hashtable[Integer, JComponent]()
      var si = 0
      while (si < speedOptions.length) { speedLabels.put(Integer.valueOf(si), new JLabel(speedOptions(si) + "x")); si += 1 }
      speedSlider.setLabelTable(speedLabels)
      speedSlider.setPaintLabels(true)
      speedSlider.setPaintTicks(true)
      speedSlider.setSnapToTicks(true)
      controls.add(new JLabel("Speed:"))
      controls.add(speedSlider)
      controls.add(new JLabel(" "))
      controls.add(autoZoom)
      controls.add(timeLabel)
      controls.add(autoReload)
      controls.add(reloadBtn)
      frame.getContentPane.add(controls, BorderLayout.SOUTH)

      frame.setSize(new Dimension(1200, 800))
      frame.setLocationByPlatform(true)
      frame.setVisible(true)

      // Timer ~24 FPS
      val playingRef = new java.util.concurrent.atomic.AtomicBoolean(true)

      // Async reload machinery
      val reloadInProgress = new java.util.concurrent.atomic.AtomicBoolean(false)
      val bgExecutor = java.util.concurrent.Executors.newSingleThreadExecutor(new java.util.concurrent.ThreadFactory {
        override def newThread(r: Runnable): Thread = {
          val t = new Thread(r, "CombatVisualizer-Reload")
          t.setDaemon(true)
          t
        }
      })
      def reloadIfChanged(force: Boolean = false): Unit = {
        val lm = simFile.lastModified()
        val lenNow = try simFile.length() catch { case _: Throwable => 0L }
        if (!(force || (autoReloadRef.get() && (lm > lastModifiedRef.get() || lenNow > fileLenRef.get())))) return
        if (!reloadInProgress.compareAndSet(false, true)) return

        bgExecutor.submit(new Runnable { override def run(): Unit = {
          val t0 = System.nanoTime()
          try {
            // Incremental read of appended bytes
            val (seg, newLen, truncated) = readAppended(simFile)
            var newBlocks = Vector.empty[String]
            if (truncated) {
              // File was truncated (new game). Reparse recent sims using streaming bounded loader to avoid OOM.
              pendingRef.set("")
              val sims = parseAllSims(simFile)
              javax.swing.SwingUtilities.invokeLater(new Runnable { override def run(): Unit = {
                try {
                  simsRef.set(sims)
                  populateSimList(sims)
                  val nonEmptySims = sims.filter(p => p.frames.nonEmpty)
                  val idxToLoad = if (nonEmptySims.nonEmpty) 0 else -1
                  if (idxToLoad >= 0) loadSimAt(idxToLoad)
                } catch { case _: Throwable => () }
              }})
              // Update length and modified time, then return (no merging needed)
              lastModifiedRef.set(lm)
              fileLenRef.set(newLen)
              val ms = (System.nanoTime() - t0) / 1000000L
              try { System.out.println(s"[CV] reload truncated; rebuilt list with ${sims.length} sims in ${ms}ms") } catch { case _: Throwable => () }
              return
            } else if (seg.nonEmpty) {
              val combined = pendingRef.get() + seg
              val (blocks, rem) = splitPW2BlocksWithRemainder(combined)
              newBlocks = blocks
              pendingRef.set(rem)
            }
            if (newBlocks.nonEmpty) {
              val parsed = newBlocks.flatMap(b => Try(SimVizParse.parsePW2(b)).toOption)
              if (parsed.nonEmpty) {
                // Merge into existing, de-duplicate, bound to last 200, sort newest first
                def keyFor(p: SimPack): String = {
                  val ids = p.metas.map(_.id).sorted.mkString(",")
                  p.startGameFrame.toString + "|" + ids
                }
                val before = simsRef.get()
                val mergedAll = parsed ++ before
                val dedupMap = new scala.collection.mutable.LinkedHashMap[String, SimPack]()
                var mi = 0
                while (mi < mergedAll.length) {
                  val sp = mergedAll(mi)
                  val k = keyFor(sp)
                  if (!dedupMap.contains(k)) dedupMap.put(k, sp)
                  mi += 1
                }
                val dedup = dedupMap.values.toVector.sortBy(s => -s.startGameFrame)
                val bounded = if (dedup.length > 200) dedup.take(200) else dedup
                javax.swing.SwingUtilities.invokeLater(new Runnable { override def run(): Unit = {
                  try {
                    simsRef.set(bounded)
                    populateSimList(bounded)
                    val hasManualSelection = !autoReloadRef.get()
                    val sel = simList.getSelectedIndex
                    val idxToLoad = if (hasManualSelection && sel >= 0 && sel < bounded.length) sel else 0
                    loadSimAt(idxToLoad)
                    try { extVisLog(s"merge parsed=${parsed.length} before=${before.length} after=${bounded.length}") } catch { case _: Throwable => }
                  } catch { case _: Throwable => () }
                }})
              }
            }
            lastModifiedRef.set(lm)
            fileLenRef.set(newLen)
            val ms = (System.nanoTime() - t0) / 1000000L
            try { System.out.println(s"[CV] reload parsed ${newBlocks.size} new blocks in ${ms}ms; sims=${simsRef.get().length}") } catch { case _: Throwable => () }
          } catch { case _: Throwable => () }
          finally { reloadInProgress.set(false) }
        }})
      }

      val timer = new javax.swing.Timer(1000 / 24, (_: java.awt.event.ActionEvent) => {
        try {
          // If no frames, attempt auto-reload opportunistically
          if (panel.frames.isEmpty) {
            if (autoReloadRef.get()) reloadIfChanged(force = false)
          }
          if (panel.frames.nonEmpty && playingRef.get()) {
            val lastIdx = panel.frames.length - 1
            val speed = {
              val idx = speedSlider.getValue
              val clamped = Math.max(0, Math.min(idx, speedOptions.length - 1))
              speedOptions(clamped)
            }
            if (panel.frameIndex >= lastIdx) {
              // At end of playback: attempt auto-reload if enabled
              if (autoReloadRef.get()) reloadIfChanged(force = false)
              panel.frameIndex = 0
            } else {
              panel.frameIndex = Math.min(lastIdx, panel.frameIndex + speed)
            }
            slider.setValue(panel.frameIndex)
          }
          updateTimeLabel()
          panel.repaint()
          tally.repaint()
        } catch { case _: Throwable => () }
      })
      timerRef = timer
      frame.addWindowListener(new java.awt.event.WindowAdapter() {
        override def windowClosed(e: java.awt.event.WindowEvent): Unit = {
          try { if (timerRef != null) timerRef.stop() } catch { case _: Throwable => }
          timerRef = null
          window = null
        }
        override def windowClosing(e: java.awt.event.WindowEvent): Unit = {
          try { if (timerRef != null) timerRef.stop() } catch { case _: Throwable => }
        }
      })

      // Wire controls
      playPause.addActionListener(_ => {
        val now = !playingRef.get()
        playingRef.set(now)
        playPause.setText(if (now) "Pause" else "Play")
      })

      autoReload.addActionListener(_ => {
        autoReloadRef.set(autoReload.isSelected)
      })

      autoZoom.addActionListener(_ => {
        panel.autoZoomEnabled = autoZoom.isSelected
        panel.repaint()
      })

      reloadBtn.addActionListener(_ => reloadIfChanged(force = true))

      slider.addChangeListener(_ => {
        val idx = slider.getValue
        panel.frameIndex = Math.max(0, Math.min(idx, Math.max(0, panel.frames.length - 1)))
        updateTimeLabel()
        panel.repaint()
      })

      // Start
      updateTimeLabel()
      timer.start()
    }})
  }
}
