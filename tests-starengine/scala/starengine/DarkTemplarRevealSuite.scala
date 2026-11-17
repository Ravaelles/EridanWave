package starengine

import Mathematics.Points.Pixel
import Micro.Actions.Protoss.DarkTemplar.DarkTemplarEvade
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * DT reveal withdraw: a Templar that is seen must leave, whether it sees
 * the eyes or not. An Observer outranges the Templar by two tiles, so the
 * tile burns (288px) before the eye is known (224px): the old brain holds
 * the hunt through that window - the detector arm never fires on an
 * unseen eye, the gun arm never fires past thirteen tiles, and the hide
 * never fires outside a threat's reach. Live DT_v_Protoss games show the
 * bill: Hunt/Hold/Retreat acts, zero evade acts, one dead Templar.
 * The fixed brain withdraws on the tile flag (mirrors tile.enemyDetected)
 * and on landed shots (mirrors isUnderAttack): flee the fix if there is
 * one, else fall back to safety. Speeds, reaches, damage and sights are
 * the real UnitType numbers; both pilots call the shipped kernels, the
 * engine owns the loop only.
 *
 * Two duels, one pilot. The pocket mirrors the user's setup (our Dragoon
 * plus Observer hold the backline out of play, three foe Dragoons guard
 * their eye): the Templar is tasked past the eye, enters detection with
 * the eye unseen and the guns beyond thirteen tiles - exactly the window
 * no old rule covers. The tank duel covers the second half: a sieged
 * tank outranges the Templar's whole eyesight, so the attacker arm can
 * never fire and only the reveal (then the landed shells) saves it.
 * Fog shots the engine cannot model (a shooter in range is always known
 * here) stay covered by the base-suite truth table instead.
 */
class DarkTemplarRevealSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.DT.name + StarEngineTags.Detector.name) }

  private def away(me: Body, x: Double, y: Double): (Double, Double) = {
    val dx = me.x - x
    val dy = me.y - y
    val dist = Math.sqrt(dx * dx + dy * dy)
    if (dist > 0) (dx / dist, dy / dist) else (0.0, 1.0)
  }

  private def pixelOf(b: Body): Pixel = new Pixel(b.x.toInt, b.y.toInt)

  /**
   * Production order, stripped to the duel: guns, latched detector
   * evade, reveal withdraw, mission. Knowledge is modeled, not
   * omniscient: only foes inside our own sight are known (mirrors
   * per-unit matchups - a shooter past 224px is fog, however real its
   * shells are); the ground-truth reveal comes from the world instead
   * (mirrors tile.enemyDetected, which the game reports but no eye
   * grants). The hunt arm is covered by the raze/mine/dither suites -
   * here the raid walks its mission, and the kernels own each refusal.
   */
  private class TemplarPilot(id: Int, goal: (Double, Double), safety: (Double, Double), useReveal: Boolean) extends Pilot {
    var firstRevealFrame: Option[Int] = None
    var observerDistAtFirst: Double = Double.PositiveInfinity
    var minGunDistAtFirst: Double = Double.PositiveInfinity
    var minObserverDist: Double = Double.PositiveInfinity
    var revealFires: Int = 0

    override def direct(me: Body, world: World): Directive = {
      val dtSight = me.kind.sightPixels.toDouble
      val guns = world.foes.filter(f => f.alive && f.kind != Protoss.Observer)
      val knownGuns = guns.filter(g => me.dist(g) <= dtSight)
      val eyes = world.foes.filter(f => f.alive && f.kind.isDetector)
      val knownEyes = eyes.filter(e => me.dist(e) <= dtSight)
      if (eyes.nonEmpty) minObserverDist = Math.min(minObserverDist, eyes.map(me.dist).min)

      // Guns outrank eyes: a known attacker inside thirteen tiles.
      val nearGuns = knownGuns.filter(g => DarkTemplarEvade.shouldFleeAttacker(me.dist(g) / 32.0))
      if (nearGuns.nonEmpty) {
        val target = nearGuns.minBy(me.dist)
        val (dx, dy) = away(me, target.x, target.y)
        return MoveTo(me.x + dx * 128.0, me.y + dy * 128.0)
      }

      // Latched evade, known detectors only: the latch is per-unit state,
      // reset once per run in the test setup - never inside the frame.
      val evade = knownEyes.filter(e => DarkTemplarEvade.evadeLatched(id, me.dist(e), e.kind.sightPixels.toDouble))
      if (evade.nonEmpty) {
        val eye = evade.minBy(me.dist)
        val (dx, dy) = away(me, eye.x, eye.y)
        return MoveTo(me.x + dx * 128.0, me.y + dy * 128.0)
      }

      // Reveal withdraw, the new rule: the tile burns or shots landed.
      if (useReveal && DarkTemplarEvade.shouldWithdrawRevealed(
        world.detectorsRevealing(me).nonEmpty, world.underAttack(me))) {
        if (firstRevealFrame.isEmpty) {
          firstRevealFrame = Some(world.frame)
          if (eyes.nonEmpty) observerDistAtFirst = eyes.map(me.dist).min
          if (guns.nonEmpty) minGunDistAtFirst = guns.map(me.dist).min
        }
        revealFires += 1
        knownEyes.sortBy(me.dist).headOption match {
          case Some(eye) =>
            val (dx, dy) = away(me, eye.x, eye.y)
            return MoveTo(me.x + dx * 128.0, me.y + dy * 128.0)
          case None =>
            return MoveTo(safety._1, safety._2)
        }
      }

      // The mission: the raid walks its goal, skirmishing only what it
      // sees - far guns are fog, and the kernels above own each refusal.
      val open = knownGuns.filterNot(g =>
        knownEyes.exists(e => DarkTemplarEvade.underDetection(
          pixelOf(g), Seq((pixelOf(e), e.kind.sightPixels.toDouble)))))
      open.sortBy(me.dist).headOption match {
        case Some(prey) => Chase(prey)
        case None       => MoveTo(goal._1, goal._2)
      }
    }
  }

  private case class Outcome(
    alive: Boolean, hpEnd: Double, revealFrame: Option[Int],
    observerDistAtFirst: Double, minGunDistAtFirst: Double, minObserverDist: Double)

  private def runPocket(useReveal: Boolean): Outcome = {
    val dt = new Body(Protoss.DarkTemplar, 0.0, -448.0, Protoss.DarkTemplar.maxTotalHealth)
    dt.cloaked = true
    val backDrag = new Body(Protoss.Dragoon, -60.0, -900.0, Protoss.Dragoon.maxTotalHealth)
    val backObs = new Body(Protoss.Observer, 60.0, -900.0, Protoss.Observer.maxTotalHealth)
    val eye = new Body(Protoss.Observer, 0.0, 0.0, Protoss.Observer.maxTotalHealth)
    val guards = Seq(
      new Body(Protoss.Dragoon, -380.0, 0.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 380.0, 0.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 0.0, 400.0, Protoss.Dragoon.maxTotalHealth))
    val pilot = new TemplarPilot(3, (0.0, 600.0), (0.0, -900.0), useReveal)
    val pilots: Map[Body, Pilot] = Map(
      dt -> pilot, backDrag -> new Pilots.Hold, backObs -> new Pilots.Hold,
      eye -> new Pilots.Hold) ++ guards.map(_ -> new Pilots.Hold)
    val world = new World(Seq(dt, backDrag, backObs), eye +: guards, pilots, Seq.empty)
    var f = 0
    while (f < 900 && dt.alive) { world.step(); f += 1 }
    Outcome(dt.alive, dt.hp, pilot.firstRevealFrame,
      pilot.observerDistAtFirst, pilot.minGunDistAtFirst, pilot.minObserverDist)
  }

  test("a Templar tasked past an unseen eye stands off outside detection") {
    val dtSight = Protoss.DarkTemplar.sightPixels.toDouble
    val eyeSight = Protoss.Observer.sightPixels.toDouble
    println(s"DIAG dtSight=$dtSight eyeSight=$eyeSight dtSpeed=${Protoss.DarkTemplar.topSpeed} dragSpeed=${Protoss.Dragoon.topSpeed}")
    assert(eyeSight > dtSight, "no sight disadvantage in the data - the unseen-eye window does not exist")

    DarkTemplarEvade.reset()
    val fixed = runPocket(useReveal = true)
    println(s"DIAG fixed alive=${fixed.alive} hp=${fixed.hpEnd.toInt} reveal@${fixed.revealFrame} obsDistFirst=${fixed.observerDistAtFirst.toInt}px gunsFirst=${fixed.minGunDistAtFirst.toInt}px minObs=${fixed.minObserverDist.toInt}px")
    assert(fixed.alive, "the Templar died walking past the eye")
    assert(fixed.hpEnd == Protoss.DarkTemplar.maxTotalHealth, "the Templar took fire it should never have met")
    assert(fixed.revealFrame.isDefined, "the reveal never fired on the way past the eye")
    assert(fixed.observerDistAtFirst > dtSight,
      "the eye was already known at the first withdraw - the latch, not the reveal, owns this save")
    assert(fixed.minGunDistAtFirst > 13 * 32.0,
      "guns were already inside thirteen tiles - the gun arm, not the reveal, owns this save")
    assert(fixed.minObserverDist > dtSight,
      "the Templar saw the eye - the standoff should hold outside its own eyesight, on the tile flag alone")

    DarkTemplarEvade.reset()
    val old = runPocket(useReveal = false)
    println(s"DIAG old alive=${old.alive} hp=${old.hpEnd.toInt} minObs=${old.minObserverDist.toInt}px")
    assert(old.minObserverDist < eyeSight, "the old brain never even enters detection - nothing to fix here")
    assert(fixed.minObserverDist > old.minObserverDist,
      "the old brain stands off no closer - the reveal changes nothing here")
  }

  test("a Templar shelled from outside its own eyesight breaks on the reveal") {
    val dtSight = Protoss.DarkTemplar.sightPixels.toDouble
    val dt = new Body(Protoss.DarkTemplar, 0.0, 0.0, Protoss.DarkTemplar.maxTotalHealth)
    dt.cloaked = true
    val backDrag = new Body(Protoss.Dragoon, -60.0, -900.0, Protoss.Dragoon.maxTotalHealth)
    val backObs = new Body(Protoss.Observer, 60.0, -900.0, Protoss.Observer.maxTotalHealth)
    val eye = new Body(Protoss.Observer, 250.0, 0.0, Protoss.Observer.maxTotalHealth)
    val tank = new Body(Terran.SiegeTankSieged, 350.0, 0.0, Terran.SiegeTankSieged.maxTotalHealth)
    println(s"DIAG tankRange=${tank.rangeAgainst(dt)} tankDmg=${tank.damageAgainst(dt)} tankCd=${tank.cooldownAgainst(dt)}")
    assert(tank.rangeAgainst(dt) > dtSight, "the tank does not outrange the Templar's eyesight - no blind duel here")
    assert(meetsBlind(dt, eye, tank, dtSight), "the eye is already known - the latch, not the reveal, would fire")

    DarkTemplarEvade.reset()
    val pilot = new TemplarPilot(5, (0.0, -900.0), (0.0, -900.0), useReveal = true)
    val pilots: Map[Body, Pilot] = Map(
      dt -> pilot, backDrag -> new Pilots.Hold, backObs -> new Pilots.Hold,
      eye -> new Pilots.Hold, tank -> new Pilots.Hold)
    val world = new World(Seq(dt, backDrag, backObs), Seq(eye, tank), pilots, Seq.empty)
    var f = 0
    while (f < 400 && dt.alive) { world.step(); f += 1 }
    println(s"DIAG tank alive=${dt.alive} hp=${dt.hp.toInt} reveal@${pilot.firstRevealFrame}")
    assert(pilot.firstRevealFrame.exists(_ <= 2), "the Templar did not break the frame it stood revealed")
    assert(dt.alive, "the Templar died under shells it saw coming on the tile flag")
    assert(dt.hp == Protoss.DarkTemplar.maxTotalHealth - tank.damageAgainst(dt),
      "the Templar should wear exactly one shell: break on the reveal, outrun the second")
  }

  private def meetsBlind(dt: Body, eye: Body, tank: Body, dtSight: Double): Boolean =
    dt.dist(eye) > dtSight && dt.dist(tank) > dtSight && dt.dist(eye) < eye.kind.sightPixels
}
