package starengine

import ProxyBwapi.Races.Protoss
import Tactic.Squads.Decisions.RetreatVerdict
import org.scalatest.funsuite.AnyFunSuite

/**
 * Verdict-bus contract (the temporal half of the unified retreat verdict):
 * the raw retreat verdict flickers with its inputs and the two readers
 * tick on skewed schedules (squad phase skips under load, unit phase does
 * not), so both readers tick one shared verdict object instead of
 * re-deriving retreat per phase. The sick leg reads the raw shipped kernel
 * on both schedules (today's production): unanimous under the march flag
 * but churning. The cured leg ticks the shipped bus: calm with clean
 * release. The 24-frame hold itself was calibrated here earlier (24 reads
 * unanimous, 96 only slower); the suite keeps the shipped constant.
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

  /** One body playing both readers on skewed phase schedules. */
  private class TwoReaderPilot(useBus: Boolean) extends Pilot {
    var mixed: Int = 0
    var flips: Int = 0
    var resumedAt: Int = -1
    var mHome: Boolean = false
    var dDuel: Boolean = false

    private def raw(frame: Int): Boolean =
      RetreatVerdict.shouldHold(
        isWarrior = true,
        isReaver = false,
        canFlee = true,
        stormHold = false,
        shouldFight(frame),
        totalHp = 160,
        unitsScore = 100.0,
        nearestArmedTiles = 40.0,
        proxyRush = false,
        hardCounterKnown = false)

    override def direct(me: Body, world: World): Directive = {
      val mTick = world.frame % 6 == 0
      val dTick = world.frame % 2 == 1
      // Both readers tick the one shipped verdict object on their own
      // schedule; the last writer wins the raw side, the hold absorbs
      // the skew. The key stands in for the unit id (one body here).
      if (useBus && (mTick || dTick)) RetreatVerdict.update(1, raw(world.frame), world.frame)
      val held = if (useBus) RetreatVerdict.held(1, world.frame) else raw(world.frame)
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

  private def runBus(useBus: Boolean, frames: Int = 500): TwoReaderPilot = {
    RetreatVerdict.reset()
    val dragoon = new Body(Protoss.Dragoon, 0.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val foe = new Body(Protoss.Dragoon, -400.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val pilot = new TwoReaderPilot(useBus)
    val world = new World(Seq(dragoon), Seq(foe),
      Map(dragoon -> pilot, foe -> new Pilots.Hold), Seq.empty)
    while (world.frame < frames && dragoon.alive) world.step()
    println(s"DIAG bus=$useBus mixed=${pilot.mixed} flips=${pilot.flips} resumedAt=${pilot.resumedAt}")
    pilot
  }

  test("skewed readers churn on the raw kernel but never mix under the march flag") {
    val pilot = runBus(useBus = false)
    assert(pilot.mixed == 0, f"march flag should rule out mixing, got ${pilot.mixed}")
    assert(pilot.flips > 60, f"repro too calm to catch the dither: ${pilot.flips} flips")
    assert(pilot.resumedAt == 310, f"raw kernel should release sharp, resumed at ${pilot.resumedAt}")
  }

  test("the shared bus keeps the readers unanimous with clean release") {
    val pilot = runBus(useBus = true)
    assert(pilot.mixed == 0, f"bus still mixed ${pilot.mixed} frames")
    assert(pilot.flips < 30, f"bus still churns: ${pilot.flips} flips")
    assert(pilot.resumedAt >= 310 && pilot.resumedAt <= 310 + RetreatVerdict.holdFrames + 6,
      f"no clean release after the facts cleared, resumed at ${pilot.resumedAt}")
  }
}
