package Debugging

import Lifecycle.With
import java.io.File

/** Split from CombatVisIO: one IO role per file. */
object CombatVisThreads {

  private[Debugging] def baseDir: File = new File(With.bwapiData.write)
  private[Debugging] def opponentFile: File = {
    val opp = With.enemy.name.replaceAll("[^A-Za-z0-9._-]", "_")
    new File(baseDir, opp + ".pwcs")
  }

  @volatile private[Debugging] var lastDumpMs: Long = 0L
  @volatile private[Debugging] var lastDumpGameFrame: Int = -1000000
  @volatile private[Debugging] var gameStartEpochSeconds: Long = 0L
  // Throttle compressed dump enqueues to avoid IO queue flooding
  private[Debugging] val dumpScheduled = new java.util.concurrent.atomic.AtomicBoolean(false)
  private[Debugging] val nextDumpAllowedMs = new java.util.concurrent.atomic.AtomicLong(0L)
  // Diagnostics: count of successful PW2 appends to .pwcs
  private[Debugging] val simAppendCounter = new java.util.concurrent.atomic.AtomicInteger(0)
  // Coalescing state for live .pwcs appends: avoid near-duplicate sims (same participants) within 10s
  @volatile private[Debugging] var lastAppendStartFrame: Int = -1000000
  @volatile private[Debugging] var lastAppendParticipantsKey: String = ""
  @volatile private[Debugging] var lastPwcsAppendGameFrame: Int = -1000000

  // Background IO workers to avoid blocking the main game thread
  // Fast lane for lightweight .pwcs appends
  private[Debugging] val ioQueue = new java.util.concurrent.LinkedBlockingQueue[Runnable]()
  private[Debugging] val ioThread: Thread = {
    val t = new Thread(new Runnable { override def run(): Unit = {
      while (true) {
        try { val r = ioQueue.take(); r.run() } catch { case _: Throwable => }
      }
    }})
    t.setName("CombatVisIO-IO-Append")
    t.setDaemon(true)
    t.start()
    t
  }
  private[Debugging] def enqueueIO(r: Runnable): Unit = { try ioQueue.offer(r) catch { case _: Throwable => } }

  // Slow lane for heavy compressed .pwsim dumps (kept separate so appends are never starved)
  private[Debugging] val dumpQueue = new java.util.concurrent.LinkedBlockingQueue[Runnable]()
  private[Debugging] val dumpThread: Thread = {
    val t = new Thread(new Runnable { override def run(): Unit = {
      while (true) {
        try { val r = dumpQueue.take(); r.run() } catch { case _: Throwable => }
      }
    }})
    t.setName("CombatVisIO-IO-Dump")
    t.setDaemon(true)
    t.start()
    t
  }
  private[Debugging] def enqueueDump(r: Runnable): Unit = { try dumpQueue.offer(r) catch { case _: Throwable => } }
}
