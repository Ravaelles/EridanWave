package starengine

import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Contain against two Sunken Colonies: the line holds outside static
 * reach while single Zerglings trickle in on a timer, and every ling
 * dies to the ring without the ring ever entering the guns. When a
 * ling lands a hit the whole ring pulls back on a shared latch and
 * walks back in after it lapses - no lone avengers, no dressing under
 * fire. Speeds, reaches and damage are the real UnitType numbers; the
 * trickle is staggered Hold-then-zombie pilots, so no engine change.
 */
class ContainSunkensSuite extends AnyFunSuite with StarEngineSuite {

  /** Shared ring latch: one hit pulls every hull back for pullbackFrames. */
  private class RingPullback(val pullbackFrames: Int) {
    var until: Int = -1
    var fired: Boolean = false

    def hit(frame: Int): Unit = {
      until = frame + pullbackFrames
      fired = true
    }

    def active(frame: Int): Boolean = frame < until
  }

  /** Hold the anchor, chase lings inside the ray, pull the ring back on a hit. */
  private class ContainHold(anchor: (Double, Double), fallback: (Double, Double), latch: RingPullback) extends Pilot {
    private var lastHp: Double = Double.PositiveInfinity

    override def direct(me: Body, world: World): Directive = {
      if (latch.active(world.frame)) {
        lastHp = me.hp

        return MoveTo(fallback._1, fallback._2)
      }

      if (me.hp < lastHp) latch.hit(world.frame)
      lastHp = me.hp
      if (latch.active(world.frame)) return MoveTo(fallback._1, fallback._2)

      world.nearestFoeOf(me, world.foes, (m, f) => ! f.kind.isBuilding) match {
        case Some(foe) if Math.hypot(foe.x - anchor._1, foe.y - anchor._2) <= 192.0 => Chase(foe)
        case _ => MoveTo(anchor._1, anchor._2)
      }
    }
  }

  /** Trickle ling: holds at the sunkens until its release frame, then zombie-attacks. */
  private class TrickleLing(releaseFrame: Int) extends Pilot {
    private val zombie = new Pilots.ZombieAttack(_.ours, (0.0, 100.0))

    override def direct(me: Body, world: World): Directive =
      if (world.frame < releaseFrame) Hold else zombie.direct(me, world)
  }

  test("the ring holds outside the guns, kills the trickle, and pulls back when hit") {
    val sunken1 = new Body(Zerg.SunkenColony, -150.0, 0.0, Zerg.SunkenColony.maxTotalHealth)
    val sunken2 = new Body(Zerg.SunkenColony, 150.0, 0.0, Zerg.SunkenColony.maxTotalHealth)
    val sunkenRange = Math.max(sunken1.rangeAgainst(new Body(Protoss.Dragoon, 0.0, 0.0, 1.0)),
      sunken2.rangeAgainst(new Body(Protoss.Dragoon, 0.0, 0.0, 1.0)))
    println(s"DIAG sunken range=$sunkenRange dragoon range=${new Body(Protoss.Dragoon, 0.0, 0.0, 1.0).rangeAgainst(sunken1)} " +
      s"dragoon dmg=${new Body(Protoss.Dragoon, 0.0, 0.0, 1.0).damageAgainst(sunken1)}")

    val starts = Seq((-150.0, 500.0), (-50.0, 500.0), (50.0, 500.0), (150.0, 500.0))
    val kinds = Seq(Protoss.Dragoon, Protoss.Dragoon, Protoss.Zealot, Protoss.Zealot)
    val latch = new RingPullback(pullbackFrames = 48)
    val ours = starts.zip(kinds).map { case ((x, y), k) => new Body(k, x, y, k.maxTotalHealth) }
    val pilots: Map[Body, Pilot] = ours.map(b =>
      b -> new ContainHold((b.x, b.y), (b.x, b.y + 160.0), latch)).toMap

    val lings = Seq(
      new Body(Zerg.Zergling, 50.0, 480.0, Zerg.Zergling.maxTotalHealth),
      new Body(Zerg.Zergling, -100.0, 100.0, Zerg.Zergling.maxTotalHealth),
      new Body(Zerg.Zergling, 0.0, 100.0, Zerg.Zergling.maxTotalHealth),
      new Body(Zerg.Zergling, 100.0, 100.0, Zerg.Zergling.maxTotalHealth),
      new Body(Zerg.Zergling, 200.0, 100.0, Zerg.Zergling.maxTotalHealth),
      new Body(Zerg.Zergling, 300.0, 100.0, Zerg.Zergling.maxTotalHealth))
    val releases = Seq(0, 200, 400, 600, 800, 1000)
    val foePilots: Map[Body, Pilot] = lings.zip(releases).map { case (l, r) => l -> new TrickleLing(r) }.toMap
    val world = new World(ours, Seq(sunken1, sunken2) ++ lings, pilots ++ foePilots +
      (sunken1 -> new Pilots.Hold) + (sunken2 -> new Pilots.Hold), Seq.empty)

    var minSunkenDist = Double.PositiveInfinity
    var latchFrame: Option[Int] = None
    def allHome: Boolean = ours.forall(o => {
      val i = ours.indexOf(o)
      Math.hypot(o.x - starts(i)._1, o.y - starts(i)._2) <= 96.0
    })
    while (world.frame < 3000 && ours.exists(_.alive) && (lings.exists(_.alive) || ! allHome)) {
      world.step()
      ours.foreach(o => Seq(sunken1, sunken2).foreach(s =>
        if (s.alive) minSunkenDist = Math.min(minSunkenDist, o.dist(s))))
      if (latchFrame.isEmpty && latch.fired) latchFrame = Some(world.frame)
    }

    val home = ours.map(o => Math.hypot(o.x - starts(ours.indexOf(o))._1, o.y - starts(ours.indexOf(o))._2))
    println(s"DIAG latch=$latchFrame minSunkenDist=${minSunkenDist.toInt}px limit=${(sunkenRange + 32).toInt}px " +
      s"lingsLeft=${lings.count(_.alive)} home=${home.map(_.toInt).mkString(",")}")
    assert(lings.forall(! _.alive), "trickle survived the ring")
    assert(ours.forall(_.alive), "ring lost a hull holding contain")
    assert(minSunkenDist >= sunkenRange + 32.0, f"ring entered the guns at ${minSunkenDist.toInt}px")
    assert(latchFrame.isDefined, "nothing ever hit the ring - pullback unproven")
    assert(home.forall(_ <= 96.0), "ring did not walk back in after the pullback")
  }
}
