package starengine

import Micro.Targeting.{FocusFire, SharedFocus}
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable

/**
 * Focus hold, step by step: a dragoon line meets dragoon packs and the
 * pack's committed pick rides through reach churn. The pilot calls the
 * shipped kernels exactly like production does (FocusFire.choose for
 * entry with the quorum gates, SharedFocus.hold for the hold) and
 * chases the committed pick. The tests pin the temporal guarantees
 * no single-frame row can: a committed pick survives max-reach
 * re-votes while anyone can still hit it, a kill hands the pack to
 * one new pick together, and a one-volley straggler preempts the
 * grind instead of waiting behind it.
 */
class FocusHoldScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Targeting.name + StarEngineTags.Dragoon.name + StarEngineTags.Zealot.name + StarEngineTags.PvP.name) }

  /** Pack brain (one object for the whole line, like a squad): the shipped kernels decide, legs chase. */
  private class PackBrain(ours: Seq[Body], foes: Seq[Body]) {

    var committed: Option[Int] = None
    val history = new mutable.ArrayBuffer[Int]()
    private var lastFrame = -1

    private def centroid(): (Double, Double) = {
      val live = ours.filter(_.alive)
      (live.map(_.x).sum / live.size, live.map(_.y).sum / live.size)
    }

    private def bids(): Seq[SharedFocus.Bid] =
      foes.zipWithIndex.collect { case (f, i) if f.alive =>
        val reach = ours.count(o => o.alive && o.dist(f) <= o.rangeAgainst(f))
        val damages = ours.filter(_.alive).map(_.damageAgainst(f))
        val avg = if (damages.nonEmpty) damages.sum.toDouble / damages.size else 0.0
        val cost = if (avg <= 0) Int.MaxValue else Math.max(1, Math.ceil(f.hp / avg).toInt)
        SharedFocus.Bid(i, reach, cost)
      }.toSeq

    /** One pick per frame, recorded once no matter how many hulls ask. */
    def pick(world: World): Option[Int] = {
      val shooters = ours.count(_.alive)
      val liveTargets = foes.count(_.alive)
      val chosen: Option[Int] =
        if (shooters < FocusFire.minShooters || liveTargets < FocusFire.minTargets) None
        else {
          val bs = bids()
          val held = SharedFocus.hold(committed.flatMap(id => bs.find(_.id == id)), bs)
          held.map(_.id).orElse {
            val (cx, cy) = centroid()
            val cands = bs.map(b =>
              FocusFire.Candidate(b.costToKill, b.reach, Math.sqrt((foes(b.id).x - cx) * (foes(b.id).x - cx) + (foes(b.id).y - cy) * (foes(b.id).y - cy)) / 32.0, b.id))
            FocusFire.choose(cands).filter(c => FocusFire.enoughReach(c.reach)).map(_.id)
          }
        }
      chosen.foreach(id => committed = Some(id))
      if (chosen.isEmpty) committed = None
      if (world.frame != lastFrame) {
        lastFrame = world.frame
        history += chosen.getOrElse(-1)
      }

      chosen
    }
  }

  private class FocusPilot(brain: PackBrain, foes: Seq[Body]) extends Pilot {
    override def direct(me: Body, world: World): Directive =
      brain.pick(world).flatMap(foes.lift).filter(_.alive).map(Chase).getOrElse {
        // No quorum yet: the pack marches at one foe together (nearest
        // to the pack centroid), never nearest-per-hull - per-hull
        // chase splits the line before contact and no target ever
        // musters quorum, which is exactly the disease under test.
        val live = world.ours.filter(_.alive)
        val cx = live.map(_.x).sum / live.size
        val cy = live.map(_.y).sum / live.size
        foes.filter(_.alive).sortBy(f => (f.x - cx) * (f.x - cx) + (f.y - cy) * (f.y - cy))
          .headOption.map(Chase).getOrElse(Hold)
      }
  }

  private def segments(picks: Seq[Int]): Int = {
    val live = picks.filter(_ >= 0)
    if (live.isEmpty) 0
    else live.sliding(2).count(w => w(0) != w(1)) + 1
  }

  test("the line holds one pick through closing reach churn") {
    val ours = (0 until 4).map(i => new Body(Protoss.Dragoon, i * 40.0, 900.0, Protoss.Dragoon.maxTotalHealth))
    // Staggered static pack: the line walks in and hulls enter reach on
    // different frames, so the max-reach crown moves while nobody dies.
    val foes = (0 until 3).map(i => new Body(Protoss.Dragoon, i * 40.0, 100.0, Protoss.Dragoon.maxTotalHealth))
    val brain = new PackBrain(ours, foes)
    val world = new World(ours, foes,
      (ours.map(_ -> new FocusPilot(brain, foes)) ++ foes.map(_ -> new Pilots.Hold)).toMap, Seq.empty)
    while (world.frame < 6000 && foes.exists(_.alive) && ours.exists(_.alive)) world.step()
    val kills = 3 - foes.count(_.alive)
    println(s"DIAG churn picks=${brain.history.distinct} segments=${segments(brain.history.toSeq)} kills=$kills")
    assert(kills == 3, s"the focused line must finish a static pack, kills=$kills")
    // Entry plus one switch per kill, no churn beyond the transitions.
    assert(segments(brain.history.toSeq) <= kills + 1, "the pack re-voted past its kills")
  }

  /** Late straggler: walks at the line from far away, arriving mid-grind. */
  private class LateStragglerPilot extends Pilot {
    override def direct(me: Body, world: World): Directive =
      world.ours.filter(_.alive).sortBy(me.dist).headOption.map(Chase).getOrElse(Hold)
  }

  test("a one-volley straggler preempts the grind") {
    // Already in contact: the grind commits on frame zero, so the only
    // storyline is the preempt, the free kill, and the return.
    val ours = (0 until 3).map(i => new Body(Protoss.Dragoon, i * 40.0, 200.0, Protoss.Dragoon.maxTotalHealth))
    // Bullet sponges: the grind never ends inside the test window, so
    // the only storyline is the preempt, the free kill, and the return.
    val grindA = new Body(Protoss.Dragoon, 0.0, 100.0, 3.0 * Protoss.Dragoon.maxTotalHealth)
    val grindB = new Body(Protoss.Dragoon, 80.0, 100.0, 3.0 * Protoss.Dragoon.maxTotalHealth)
    // Five hit points far off to the side: out of reach at first
    // contact, walking into the grind once it is joined.
    val straggler = new Body(Protoss.Zealot, 600.0, 200.0, 5.0)
    val foes = Seq(grindA, grindB, straggler)
    val brain = new PackBrain(ours, foes)
    val world = new World(ours, foes,
      (ours.map(_ -> new FocusPilot(brain, foes)) ++
        Seq(grindA -> new Pilots.Hold, grindB -> new Pilots.Hold, straggler -> new LateStragglerPilot)).toMap, Seq.empty)
    while (world.frame < 6000 && straggler.alive && ours.exists(_.alive)) world.step()
    val stragglerDeath = if (straggler.alive) -1 else world.frame
    // Let the release settle: the pick must return to the grind.
    var f = 0
    while (f < 100 && ours.exists(_.alive)) { world.step(); f += 1 }
    val firstStragglerPick = brain.history.indexWhere(_ == 2)
    val firstLivePick = brain.history.indexWhere(_ >= 0)
    val postKill = if (stragglerDeath < 0) Seq.empty else brain.history.dropWhile(_ != 2).drop(1).takeWhile(_ != 2)
    println(s"DIAG preempt stragglerDeath=$stragglerDeath firstPick=$firstStragglerPick postKill=$postKill")
    assert(! straggler.alive, "the free kill must die")
    assert(firstLivePick >= 0 && brain.history(firstLivePick) != 2, "the grind must own the opening")
    assert(firstStragglerPick > firstLivePick, "the grind must own the early frames, the preempt the later ones")
    assert(brain.history.take(firstStragglerPick).exists(id => id == 0 || id == 1), "the grind must be committed before the preempt")
    assert(postKill.forall(id => id < 0 || id == 0 || id == 1), "after the free kill the pack returns to the grind")
  }
}
