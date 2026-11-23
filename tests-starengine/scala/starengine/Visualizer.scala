package starengine

import java.awt.{BasicStroke, Color, Dimension, Font, Graphics, Graphics2D}
import javax.swing.{JFrame, JPanel, SwingUtilities}
import scala.collection.mutable

/**
 * Opt-in live view for StarEngine scenarios (owner order): one Swing
 * window showing every live body plus its current order - MoveTo and
 * Chase draw the movement arrow toward the order destination, Hold
 * draws none, and every hull carries its kind, order and hit points,
 * the same active-behavior readout `run.sh --visible` paints on the
 * game. Buildings render their sprite centered on the building center,
 * mobiles stay vector dots. Speed lives on the keyboard: space pauses,
 * +/- steps the frame delay, 0 full speed, q closes the window and lets
 * the run finish headless, Escape or the window X quits the whole app.
 * Enabled with `scripts/starengine.sh --watch` (or STARENGINE_VISUALIZE=1
 * / -Dstarengine.visualize=true); headless environments silently skip
 * it. The scenarios never wait for the window: painting is fire-and-
 * forget on the test thread, and an idle watchdog disposes a forgotten
 * frame so no zombie window ever pins its JVM.
 *
 * Test-only harness. Production code must never reference it.
 */
object Visualizer {

  private lazy val enabledFlag: Boolean =
    java.lang.Boolean.getBoolean("starengine.visualize") ||
      sys.env.contains("STARENGINE_VISUALIZE")

  def enabled: Boolean = enabledFlag

  /** Sprite home, mirroring Atlantis img/starengine/units; unset to draw boxes. */
  private lazy val spriteDir: Option[java.io.File] = {
    val dir = new java.io.File(sys.env.getOrElse("STARENGINE_SPRITES", "/sc-ai/Atlantis/img/starengine/units"))
    if (dir.isDirectory) Some(dir) else None
  }

  private val sprites = new mutable.HashMap[String, java.awt.Image]()

  /** Building sprite by unit key, centered later on the building center. Missing files stay missing. */
  private def spriteFor(key: String): Option[java.awt.Image] = {
    if (sprites.contains(key)) return Option(sprites(key)).filter(_ != null)

    val loaded = try {
      spriteDir.flatMap(dir => {
        val file = new java.io.File(dir, key + ".png")
        if (file.isFile) Some(javax.imageio.ImageIO.read(file)) else None
      })
    } catch { case _: Throwable => None }
    sprites(key) = loaded.orNull
    loaded
  }

  @volatile private var delayMs: Long = 10
  @volatile private var paused: Boolean = false
  @volatile private var title: String = "StarEngine"
  @volatile private var paintedFrames: Long = 0
  @volatile private var lastFrameNanos: Long = 0L
  @volatile private var saidHello: Boolean = false
  @volatile private var watchdogStarted: Boolean = false

  private val pauseLock = new Object
  private var frame: JFrame = _
  private var panel: ViewPanel = _

  /** Suite hook: names the active game. Never throws into the test. */
  def testStarting(suite: String, test: String): Unit = {
    if ( ! enabled) return

    title = suite + "." + test
  }

  /**
   * One world step painted, if visualization is on. Never throws into
   * the scenario: a dead window, a closed display, anything - the test
   * owns the verdict, this owns pixels.
   */
  def frame(world: World): Unit = {
    if ( ! enabled) return
    if (java.awt.GraphicsEnvironment.isHeadless) return

    lastFrameNanos = System.nanoTime()

    val view =
      try snapshot(world)
      catch { case _: Throwable => return }
    try {
      SwingUtilities.invokeLater(new Runnable {
        override def run(): Unit = {
          try {
            ensureWindow()
            panel.setView(view)
            paintedFrames += 1
          } catch { case _: Throwable => }
        }
      })
    } catch { case _: Throwable => return }
    if ( ! saidHello) {
      saidHello = true
      println("[visual] StarEngine window open (space=pause, +/-=speed, 0=full, q=quit visual, Esc=quit app)")
    }
    try {
      pauseLock.synchronized {
        while (paused && frame != null) pauseLock.wait(50)
      }
      val sleep = delayMs
      if (sleep > 0) Thread.sleep(sleep)
    } catch { case _: Throwable => }
  }

  /** Snapshot of one world frame: pure data, no window needed. */
  def snapshot(world: World): ViewModel = {
    val marks = mutable.ArrayBuffer[Mark]()
    world.ours.view.filter(_.alive).foreach(b => marks += mark(b, ours = true, world.orderOf(b)))
    world.foes.view.filter(_.alive).foreach(b => marks += mark(b, ours = false, world.orderOf(b)))
    ViewModel(title, world.frame, marks.toVector)
  }

  private def mark(body: Body, ours: Boolean, order: Option[Directive]): Mark = {
    val kind = shortKind(body.kind.toString)
    val (label, arrow) = order match {
      case Some(MoveTo(x, y))   => ("Move", Some((x, y)))
      case Some(Chase(t))       => ("Chase " + shortKind(t.kind.toString), Some((t.x, t.y)))
      case Some(Board(t))       => ("Board", Some((t.x, t.y)))
      case Some(CastStorm(x, y)) => ("Storm " + x.toInt + "," + y.toInt, Some((x, y)))
      case Some(Unload)         => ("Unload", None)
      case Some(Hold)           => ("Hold", None)
      case None                 => ("-", None)
    }
    Mark(body.x, body.y, kind, ours, body.hp, body.maxHp, body.cloaked, label, arrow, body.kind.isBuilding, body.kind.asString)
  }

  private def shortKind(name: String): String = {
    val base = name.split('.').lastOption.getOrElse(name)
    base.replaceFirst("^(Protoss|Terran|Zerg|Neutral)_", "")
  }

  private def ensureWindow(): Unit = {
    if (frame != null) return

    panel = new ViewPanel
    frame = new JFrame(title)
    frame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE)
    frame.add(panel)
    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.addWindowListener(new java.awt.event.WindowAdapter {
      override def windowClosing(e: java.awt.event.WindowEvent): Unit = quitApp()
      override def windowClosed(e: java.awt.event.WindowEvent): Unit = teardown()
    })
    panel.addKeyListener(new java.awt.event.KeyAdapter {
      override def keyPressed(e: java.awt.event.KeyEvent): Unit = {
        if (e.getKeyCode == java.awt.event.KeyEvent.VK_ESCAPE) quitApp()
        else onKey(e.getKeyChar)
      }
    })
    panel.setFocusable(true)
    frame.setVisible(true)
    panel.requestFocusInWindow()
    startWatchdog()
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      override def run(): Unit = {
        try {
          println("[visual] painted " + paintedFrames + " frames")
          if (frame != null) frame.dispose()
        } catch { case _: Throwable => }
      }
    }))
  }

  /**
   * Idle watchdog: a forgotten window (crashed run, killed loop) must
   * never pin its JVM - with no frames for two minutes and no paused
   * inspection going on, the frame goes away on its own. Daemon, so it
   * never holds the JVM either.
   */
  private def startWatchdog(): Unit = pauseLock.synchronized {
    if (watchdogStarted) return

    watchdogStarted = true
    val watch = new Thread(new Runnable {
      override def run(): Unit = {
        while (true) {
          try Thread.sleep(30000)
          catch { case _: InterruptedException => return }
          if ( ! enabled) return

          val idleNanos = System.nanoTime() - lastFrameNanos
          if (frame != null && ! paused && idleNanos > 120000000000L) {
            try {
              println("[visual] idle 120s with no frames - closing the window")
              frame.dispose()
            } catch { case _: Throwable => }
            teardown()

            return
          }

          if (frame == null && ! paused) return

        }
      }
    })
    watch.setDaemon(true)
    watch.start()
  }

  private def teardown(): Unit = {
    pauseLock.synchronized {
      frame = null
      panel = null
      paused = false
      watchdogStarted = false
      pauseLock.notifyAll()
    }
  }

  /** Escape or the window X quits the whole app: there is no other way out of a live run. */
  private def quitApp(): Unit = {
    try {
      println("[visual] quitting on user request")
      if (frame != null) frame.dispose()
    } catch { case _: Throwable => }
    teardown()
    sys.exit(0)
  }

  private def onKey(c: Char): Unit = c match {
    case ' ' =>
      paused = ! paused
      pauseLock.synchronized { pauseLock.notifyAll() }
    case '+' | '=' => delayMs = math.max(0, delayMs - 5)
    case '-' | '_' => delayMs = delayMs + 5
    case '0' | 'f' | 'F' => delayMs = 0
    case 'q' | 'Q' =>
      try { if (frame != null) frame.dispose() } catch { case _: Throwable => }
      teardown()
    case _ =>
  }

  final case class Mark(
    x: Double,
    y: Double,
    kind: String,
    ours: Boolean,
    hp: Double,
    maxHp: Double,
    cloaked: Boolean,
    order: String,
    arrowTo: Option[(Double, Double)],
    building: Boolean,
    spriteKey: String)

  final case class ViewModel(title: String, frame: Int, bodies: Vector[Mark])

  private final class ViewPanel extends JPanel {
    private val MaxPx = 940
    @volatile private var view: ViewModel = ViewModel(title, 0, Vector.empty)

    def setView(v: ViewModel): Unit = {
      view = v
      repaint()
    }

    override def getPreferredSize: Dimension = new Dimension(MaxPx, MaxPx + 44)

    override protected def paintComponent(g: Graphics): Unit = {
      super.paintComponent(g)
      val v = view
      val gg = g.asInstanceOf[Graphics2D]
      gg.setColor(Color.BLACK)
      gg.fillRect(0, 0, getWidth, getHeight)
      if (v.bodies.isEmpty) {
        gg.setColor(Color.GRAY)
        gg.drawString(v.title + " f" + v.frame + " (no live bodies)", 8, 16)

        return
      }

      var minX = Double.PositiveInfinity
      var maxX = Double.NegativeInfinity
      var minY = Double.PositiveInfinity
      var maxY = Double.NegativeInfinity
      v.bodies.foreach { b =>
        minX = math.min(minX, b.x)
        maxX = math.max(maxX, b.x)
        minY = math.min(minY, b.y)
        maxY = math.max(maxY, b.y)
      }
      val pad = 240.0
      minX -= pad
      maxX += pad
      minY -= pad
      maxY += pad
      val scale = math.min(MaxPx / math.max(1.0, maxX - minX), MaxPx / math.max(1.0, maxY - minY))
      def sx(x: Double): Int = ((x - minX) * scale).toInt
      def sy(y: Double): Int = ((y - minY) * scale).toInt

      gg.setColor(new Color(30, 30, 36))
      var gx = (math.floor(minX / 64.0) * 64.0).toInt
      while (gx <= maxX) {
        var gy = (math.floor(minY / 64.0) * 64.0).toInt
        while (gy <= maxY) {
          gg.fillRect(sx(gx) - 1, sy(gy) - 1, 2, 2)
          gy += 64
        }
        gx += 64
      }

      gg.setStroke(new BasicStroke(1.5f))
      v.bodies.foreach { b =>
        b.arrowTo.foreach { case (ax, ay) =>
          if (b.ours) gg.setColor(new Color(60, 200, 200)) else gg.setColor(new Color(220, 90, 90))
          val x1 = sx(b.x)
          val y1 = sy(b.y)
          val x2 = sx(ax)
          val y2 = sy(ay)
          gg.drawLine(x1, y1, x2, y2)
          val ang = math.atan2(y2 - y1, x2 - x1)
          val hs = 7.0
          gg.drawLine(x2, y2, (x2 - hs * math.cos(ang - 0.45)).toInt, (y2 - hs * math.sin(ang - 0.45)).toInt)
          gg.drawLine(x2, y2, (x2 - hs * math.cos(ang + 0.45)).toInt, (y2 - hs * math.sin(ang + 0.45)).toInt)
        }
      }

      val font = new Font(Font.MONOSPACED, Font.PLAIN, 11)
      gg.setFont(font)
      v.bodies.foreach { b =>
        if (b.ours) gg.setColor(new Color(120, 230, 230)) else gg.setColor(new Color(255, 120, 120))
        val px = sx(b.x)
        val py = sy(b.y)
        // Buildings render their sprite centered on the building center;
        // mobiles stay vector dots. Missing files fall back to boxes.
        val sprited = b.building && spriteFor(b.spriteKey).exists(img => {
          gg.drawImage(img, px - img.getWidth(null) / 2, py - img.getHeight(null) / 2, null)
          true
        })
        if ( ! sprited) {
          if (b.building) {
            gg.drawRect(px - 8, py - 8, 16, 16)
          } else if (b.cloaked) gg.drawOval(px - 4, py - 4, 8, 8)
          else gg.fillOval(px - 4, py - 4, 8, 8)
        }
        gg.setColor(Color.WHITE)
        gg.drawString(b.kind + " " + b.order + " " + b.hp.toInt + "/" + b.maxHp.toInt, px + 7, py - 5)
      }

      gg.setColor(Color.GRAY)
      val delay = delayMs
      val state = if (paused) "PAUSED" else "delay " + delay + "ms"
      gg.drawString(v.title + " f" + v.frame + "  [" + state + "]", 8, MaxPx + 16)
      gg.drawString("space=pause +/-=speed 0=full q=quit visual Esc=quit app", 8, MaxPx + 32)
      frame match {
        case null =>
        case f => f.setTitle(v.title + " f" + v.frame)
      }
    }
  }
}
