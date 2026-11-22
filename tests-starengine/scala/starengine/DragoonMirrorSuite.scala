package starengine

import Micro.Actions.Combat.Decisioning.Kiting.{KiteAway, KiteHold}
import Micro.Actions.Protoss.Dragoon.DragoonPvpRetreat
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Early PvP mirror, step by step: our Dragoons meet enemy Dragoons with
 * no upgrades anywhere. The shipped DragoonPvpRetreat kernel owns the
 * call on plain counts - an outmatched handful walks home to the
 * middle instead of trading into a superior mirror, while a
 * two-to-one pack closes and kills. Speeds, reaches and damage are
 * the real UnitType numbers; the retreat call is the shipped kernel.
 */
class DragoonMirrorSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Dragoon.name + StarEngineTags.Pressure.name + StarEngineTags.Retreat.name + StarEngineTags.PvP.name) }

  private val home = (-400.0, 500.0)

  /** Pilot mirroring the production path: kernel decides, legs execute. */
  private class MirrorPilot extends Pilot {
    var retreats = 0
    var engages = 0

    override def direct(me: Body, world: World): Directive = {
      val ours = world.ours.count(o => o.alive && o.kind == Protoss.Dragoon)
      val foes = world.foes.filter(_.alive)
      val retreat = DragoonPvpRetreat.shouldRetreat(
        isDragoon = me.kind == Protoss.Dragoon,
        enemyIsProtoss = true,
        oursDragoons = ours,
        enemyHasDragoons = foes.exists(_.kind == Protoss.Dragoon),
        usUnits = ours.toDouble,
        foeUnits = foes.size.toDouble)
      if (retreat) {
        retreats += 1
        val dx = me.x - home._1
        val dy = me.y - home._2
        val dist = Math.sqrt(dx * dx + dy * dy)
        if (dist > 0) MoveTo(me.x + dx / dist * 256.0, me.y + dy / dist * 256.0)
        else Hold
      } else {
        engages += 1
        foes.sortBy(me.dist).headOption.map(Chase).getOrElse(Hold)
      }
    }
  }

  private def runMirror(ours: Int, foes: Int, frames: Int = 3000): (Seq[Body], Seq[Body], MirrorPilot, Int) = {
    val oursBodies = (0 until ours).map(i =>
      new Body(Protoss.Dragoon, 0.0 - i * 40.0, 500.0, Protoss.Dragoon.maxTotalHealth))
    val foeBodies = (0 until foes).map(i =>
      new Body(Protoss.Dragoon, 400.0 + i * 40.0, 500.0, Protoss.Dragoon.maxTotalHealth))
    val pilot = new MirrorPilot
    val world = new World(oursBodies, foeBodies,
      (oursBodies.map(_ -> pilot) ++ foeBodies.map(_ -> new Pilots.Hold)).toMap, Seq.empty)

    var f = 0
    while (f < frames && oursBodies.exists(_.alive) && foeBodies.exists(_.alive)) {
      world.step()
      f = world.frame
    }

    (oursBodies, foeBodies, pilot, f)
  }

  test("an outmatched handful walks home instead of trading") {
    val (ours, foes, pilot, frames) = runMirror(3, 5)
    println(s"DIAG retreat frames=$frames retreats=${pilot.retreats} engages=${pilot.engages} ours=${ours.count(_.alive)}/3 foes=${foes.count(_.alive)}/5")
    assert(pilot.engages == 0, "outmatched pack engaged instead of leaving")
    assert(ours.forall(_.alive), "outmatched pack fed the mirror")
  }

  test("a two-to-one pack closes and clears the mirror") {
    val (ours, foes, pilot, frames) = runMirror(4, 2, 4000)
    println(s"DIAG fight frames=$frames retreats=${pilot.retreats} engages=${pilot.engages} ours=${ours.count(_.alive)}/4 foes=${foes.count(_.alive)}/2")
    assert(pilot.retreats == 0, "overmatching pack walked home")
    assert(foes.forall(! _.alive), "overmatching pack never cleared the mirror")
  }

  /**
   * Loaded volleys beat kiting the mirror (owner order, ZealDrag):
   * a fresh loaded hull with nothing in contact holds and shoots
   * instead of yielding ground it can fight from, and a loaded gun
   * inside its reach plants instead of donating the return trip.
   * Pilot composes the shipped kernels in shipped order (hold fire,
   * plant, ladder); the kite-happy baseline kites whenever cooling.
   * Both seats, kills decide.
   */
  private class HoldVolleysPilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val foe = world.nearestFoeOf(me, world.foes, (_, _) => true)
      val contactClose = foe.exists(f => me.dist(f) <= KiteAway.steadyContactTiles * 32.0)
      if (KiteAway.holdsFire(me.cooldownLeft, me.hp.toInt, contactClose)) return Hold

      val hittableNow = foe.exists(f => me.dist(f) <= me.rangeAgainst(f))
      val meleeNear = foe.exists(f => f.kind.melee && me.dist(f) <= KiteAway.enemyMarginTiles * 32.0)
      val shieldPoints = if (me.hp >= me.maxHp) 80 else 60
      if (KiteHold.shouldPlant(me.cooldownLeft, hittableNow, shieldPoints, maxShields = 80, meleeNear)) return Hold

      val inMargin = foe.exists(f => me.dist(f) <= me.rangeAgainst(f) + KiteAway.enemyMarginTiles * 32.0)
      if (KiteAway.shouldRetreat(me.hp.toInt, me.cooldownLeft, inMargin, meleeNear)) {
        val f = foe.get
        val dx = me.x - f.x
        val dy = me.y - f.y
        val d = Math.sqrt(dx * dx + dy * dy)
        if (d > 0) MoveTo(me.x + dx / d * 128.0, me.y + dy / d * 128.0) else Hold
      } else foe.map(Chase).getOrElse(Hold)
    }
  }

  private class AlwaysKitePilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val foe = world.nearestFoeOf(me, world.foes, (_, _) => true)
      if (foe.isDefined && me.cooldownLeft > 0) {
        val f = foe.get
        val dx = me.x - f.x
        val dy = me.y - f.y
        val d = Math.sqrt(dx * dx + dy * dy)
        if (d > 0) MoveTo(me.x + dx / d * 128.0, me.y + dy / d * 128.0) else Hold
      } else foe.map(Chase).getOrElse(Hold)
    }
  }

  private def lineDuel(oursP: Body => Pilot, foeP: Body => Pilot, frames: Int = 4000, n: Int = 3): (Int, Int, Int) = {
    val ours = (0 until n).map(i => new Body(Protoss.Dragoon, i * 40.0, 500.0, Protoss.Dragoon.maxTotalHealth))
    val foes = (0 until n).map(i => new Body(Protoss.Dragoon, 400.0 + i * 40.0, 500.0, Protoss.Dragoon.maxTotalHealth))
    val world = new World(ours, foes, (ours.map(o => o -> oursP(o)) ++ foes.map(f => f -> foeP(f))).toMap, Seq.empty)
    var f = 0
    while (f < frames && ours.exists(_.alive) && foes.exists(_.alive)) {
      world.step()
      f = world.frame
    }
    (n - foes.count(_.alive), n - ours.count(_.alive), f)
  }

  test("holding loaded volleys beats kiting the mirror, both seats") {
    val (holdKillsA, kiteKillsA, holdFramesA) = lineDuel(_ => new HoldVolleysPilot, _ => new AlwaysKitePilot)
    val (kiteKillsB, holdKillsB, kiteFramesB) = lineDuel(_ => new AlwaysKitePilot, _ => new HoldVolleysPilot)
    println(s"DIAG hold-seat holdKills=$holdKillsA kiteKills=$kiteKillsA frames=$holdFramesA; kite-seat kiteKills=$kiteKillsB holdKills=$holdKillsB frames=$kiteFramesB")
    assert(holdKillsA + holdKillsB >= kiteKillsA + kiteKillsB, "holding loaded volleys must not trade down to kiting on cooldown")
    assert(holdFramesA < kiteFramesB, "holding must clear the mirror faster than kiting it - the return trips are the wasted frames")
  }

  test("1v1: the lone hull holds instead of kiting, both seats") {
    val (holdKillsA, kiteKillsA, _) = lineDuel(_ => new HoldVolleysPilot, _ => new AlwaysKitePilot, n = 1)
    val (kiteKillsB, holdKillsB, _) = lineDuel(_ => new AlwaysKitePilot, _ => new HoldVolleysPilot, n = 1)
    println(s"DIAG 1v1 hold-seat holdKills=$holdKillsA kiteKills=$kiteKillsA; kite-seat kiteKills=$kiteKillsB holdKills=$holdKillsB")
    assert(holdKillsA + holdKillsB >= kiteKillsA + kiteKillsB, "a lone hull must not trade down by kiting its mirror")
  }
}
