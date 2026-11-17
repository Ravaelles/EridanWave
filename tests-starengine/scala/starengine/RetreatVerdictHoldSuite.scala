package starengine

import ProxyBwapi.Races.Protoss
import Tactic.Squads.Decisions.{RetreatHold, RetreatVerdict}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Verdict-hold calibration (StarEngine answer to the 24f-vs-96f open
 * question): the raw retreat verdict flickers with its inputs and the
 * two readers tick on skewed schedules (squad phase skips under load,
 * unit phase does not), so a frame where the march walks home while
 * the duel closes in is structural, not a threshold bug. The pilot
 * below plays both readers against one shared hold bus fed by the
 * shipped RetreatVerdict kernel on jittered facts: with no hold the
 * skewed readers mix home and duel frames (the sick leg proves the
 * scenario catches the disease); with a 24-frame or 96-frame hold the
 * mix is zero and both readers resume cleanly once the facts clear.
 */
class RetreatVerdictHoldSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Retreat.name + StarEngineTags.March.name) }

  private val home = (600.0, 500.0)

  /** Measured shape: steady fight, a flicker window, steady fight again. */
  private def shouldFight(frame: Int): Boolean =
    if (frame < 10) true
    else if (frame < 310) (frame / 7) % 2 == 0
    else true

  /** One verdict object shared by both readers: the production shape. */
  private class HoldBus {
    var lastRaw: Boolean = false
    var edgeAge: Int = 999999

    def update(raw: Boolean): Unit = {
      if (raw != lastRaw) {
        lastRaw = raw
        if ( ! raw) edgeAge = 0
      }
    }

    def tick(): Unit = {
      if ( ! lastRaw && edgeAge < 1000000) edgeAge += 1
    }

    def held(holdFrames: Int): Boolean = RetreatHold.held(lastRaw, edgeAge, holdFrames)
  }

  /** One body playing both readers on skewed phase schedules. */
  private class TwoReaderPilot(bus: HoldBus, holdFrames: Int) extends Pilot {
    var mixed: Int = 0
    var flips: Int = 0
    var resumedAt: Int = -1
    var mHome: Boolean = false
    var dDuel: Boolean = false

    override def direct(me: Body, world: World): Directive = {
      val mTick = world.frame % 6 == 0
      val dTick = world.frame % 2 == 1
      if (mTick || dTick) {
        val raw = RetreatVerdict.shouldHold(
          isWarrior = true,
          isReaver = false,
          canFlee = true,
          stormHold = false,
          shouldFight(frame = world.frame),
          totalHp = 160,
          unitsScore = 100.0,
          nearestArmedTiles = 40.0,
          proxyRush = false)
        bus.update(raw)
      }
      bus.tick()
      val held = bus.held(holdFrames)
      // Duel obeys the march flag (the engage-gate shape): the home-bound
      // reader clears first on its own schedule, the duel reader resumes
      // late, never early. Late re-engage is the safe direction.
      if (mTick) {
        if (mHome != held) flips += 1
        mHome = held
      }
      val duelNow = ! held && ! mHome
      if (dTick) {
        if (dDuel != duelNow) flips += 1
        dDuel = duelNow
      }
      if (world.frame >= 310 && resumedAt < 0 && ! held) resumedAt = world.frame
      if (mHome && dDuel) mixed += 1
      if (mHome) MoveTo(home._1, home._2)
      else if (dDuel) world.foes.find(_.alive).map(Chase).getOrElse(Hold)
      else Hold
    }
  }

  private def runHold(holdFrames: Int, frames: Int = 500): TwoReaderPilot = {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val foe = new Body(Protoss.Dragoon, -400.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val pilot = new TwoReaderPilot(new HoldBus, holdFrames)
    val world = new World(Seq(dragoon), Seq(foe),
      Map(dragoon -> pilot, foe -> new Pilots.Hold), Seq.empty)
    while (world.frame < frames && dragoon.alive) world.step()
    println(s"DIAG hold=$holdFrames mixed=${pilot.mixed} flips=${pilot.flips} resumedAt=${pilot.resumedAt}")
    pilot
  }

  test("skewed readers churn without a hold but never mix under the march flag") {
    val pilot = runHold(holdFrames = 0)
    assert(pilot.mixed == 0, f"march flag should rule out mixing, got ${pilot.mixed}")
    assert(pilot.flips > 60, f"repro too calm to catch the dither: ${pilot.flips} flips")
  }

  test("a 24-frame hold keeps the readers unanimous with clean release") {
    val pilot = runHold(holdFrames = 24)
    assert(pilot.mixed == 0, f"24f hold still mixed ${pilot.mixed} frames")
    assert(pilot.flips < 30, f"24f hold still churns: ${pilot.flips} flips")
    assert(pilot.resumedAt >= 310 && pilot.resumedAt <= 310 + 24 + 6,
      f"no clean release after the facts cleared, resumed at ${pilot.resumedAt}")
  }

  test("a 96-frame hold keeps the readers unanimous with slower release") {
    val pilot = runHold(holdFrames = 96)
    assert(pilot.mixed == 0, f"96f hold still mixed ${pilot.mixed} frames")
    assert(pilot.flips < 30, f"96f hold still churns: ${pilot.flips} flips")
    assert(pilot.resumedAt >= 310 && pilot.resumedAt <= 310 + 96 + 6,
      f"no clean release after the facts cleared, resumed at ${pilot.resumedAt}")
  }
}
