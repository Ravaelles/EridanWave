package starengine

import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Reaver_v_Sunkens map-scale probe, stage by stage: one Reaver plus five
 * Dragoons (the live escort) walk into 25 Zerglings screening three Sunken
 * Colonies. Live the bot loses every game (best DEFEAT 10/7); the shipped
 * targeting flap is fixed and verified, so this probe asks the numbers
 * question the logs cannot answer: with infinite scarabs, does the shape
 * win, and how many reaver shots does the winning line cost?
 *
 * The shot count is the verdict carrier: the live hull carries five
 * scarabs, so a line that needs more than five is unwinnable no matter
 * the decisions, while a line inside five points at decisions (focus,
 * survival, approach) instead of economy. Scale and column pin the
 * winning shape with ammo to spare; capped5 and kite pin the wall -
 * five scarabs cannot finish three sunkens, and the shipped kite
 * kernel does not save the escort against faster melee on an open
 * plane. If engine damage or the live scarab economy ever changes,
 * these rows go red and the wall gets re-measured.
 */
class ReaverSunkensScaleSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Reaver.name + StarEngineTags.Static.name) }

  /** Reaver discipline: mobiles first, then walk each static down to max range. */
  private class StandPilot(anchor: (Double, Double), ammo: Option[Ammo] = None) extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      // Scarab scarcity probe: the live hull carries five and builds them
      // slowly; the engine fires forever. An empty hull goes passive, the
      // way loaded cargo rides out the fight without guns.
      if (ammo.exists(_.empty)) {
        me.passive = true

        return Hold
      }

      val mobiles = world.foes.filter(f => f.alive && ! f.kind.isBuilding)
      if (mobiles.nonEmpty) {
        val target = mobiles.minBy(me.dist)
        if (me.dist(target) <= me.rangeAgainst(target)) Hold else MoveTo(anchor._1, anchor._2)
      } else {
        val statics = world.foes.filter(f => f.alive && f.kind.isBuilding)
        if (statics.isEmpty) Hold
        else {
          val s = statics.minBy(me.dist)
          val want = me.rangeAgainst(s) - 16.0
          val d = me.dist(s)
          if (d <= want) Hold
          else MoveTo(s.x + (me.x - s.x) / d * want, s.y + (me.y - s.y) / d * want)
        }
      }
    }
  }

  /** Scarab scarcity probe: the live hull carries five, the engine fires forever. */
  private class Ammo(cap: Int) {
    var spent: Int = 0

    def empty: Boolean = spent >= cap
  }

  /**
   * Kite pilot behind the shipped kernel: the movement half of kiting
   * (the engine fires on its own). Retreat vectors come from
   * KiteAway.shouldRetreat on live facts; everything else delegates to
   * plain attack-move, so any survival gap against AttackMove is the
   * kernel's, not the harness's.
   */
  private class KitePilot(fallback: (Double, Double)) extends Pilot {
    import Micro.Actions.Combat.Decisioning.Kiting.KiteAway

    private val attack = new Pilots.AttackMove(_.foes, (_, _) => true, fallback)

    override def direct(me: Body, world: World): Directive = {
      val foe = world.nearestFoeOf(me, world.foes, (_, _) => true)
      val enemyInMargin = foe.exists(f => me.dist(f) <= me.rangeAgainst(f) + KiteAway.enemyMarginTiles * 32.0)
      val hasMeleeNearby = foe.exists(f => ! f.kind.isBuilding && me.dist(f) <= 96.0)
      if (foe.isDefined && KiteAway.shouldRetreat(me.hp.toInt, me.cooldownLeft, enemyInMargin, hasMeleeNearby)) {
        val f = foe.get
        val dx = me.x - f.x
        val dy = me.y - f.y
        val d = Math.sqrt(dx * dx + dy * dy)
        if (d > 0) MoveTo(me.x + dx / d * 96.0, me.y + dy / d * 96.0) else Hold
      } else attack.direct(me, world)
    }
  }

  private case class Outcome(lingsLeft: Int, sunkensLeft: Int, goonsLeft: Int, reaverHp: Int, shots: Int, frames: Int)

  private def runScenario(lings: Seq[Body], ammo: Option[Ammo], kite: Boolean = false): Outcome = {
    val reaver = new Body(Protoss.Reaver, 0.0, 1100.0, Protoss.Reaver.maxTotalHealth)
    val dragoons = (-2 to 2).map(i =>
      new Body(Protoss.Dragoon, i * 100.0, 1150.0, Protoss.Dragoon.maxTotalHealth))
    val obs = new Body(Protoss.Observer, 0.0, 1300.0, Protoss.Observer.maxTotalHealth)
    val sunkens = Seq(-150.0, 0.0, 150.0).map(x =>
      new Body(Zerg.SunkenColony, x, 200.0, Zerg.SunkenColony.maxTotalHealth))

    val ours: Seq[Body] = Seq(reaver) ++ dragoons :+ obs
    val foes: Seq[Body] = lings ++ sunkens
    val pilots: Map[Body, Pilot] = Map(
      reaver -> new StandPilot((0.0, 1100.0), ammo),
      obs -> new Pilots.Hold) ++
      dragoons.map(d =>
        if (kite) d -> new KitePilot((0.0, 200.0))
        else d -> new Pilots.AttackMove(_.foes, (_, _) => true, (0.0, 200.0))).toMap ++
      lings.map(_ -> new Pilots.ZombieAttack(_.ours, (0.0, 1100.0))).toMap ++
      sunkens.map(_ -> new Pilots.Hold).toMap
    val world = new World(ours, foes, pilots, Seq.empty)

    var shots = 0
    while (world.frame < 12000 && foes.exists(_.alive) && (reaver.alive || dragoons.exists(_.alive))) {
      val loaded = reaver.cooldownLeft == 0 && ammo.forall(! _.empty)
      world.step()
      if (loaded && reaver.cooldownLeft > 0) {
        shots += 1
        ammo.foreach(_.spent += 1)
      }
    }
    Outcome(
      lingsLeft = lings.count(_.alive),
      sunkensLeft = sunkens.count(_.alive),
      goonsLeft = dragoons.count(_.alive),
      reaverHp = reaver.hp.toInt,
      shots = shots,
      frames = world.frame)
  }

  private def report(tag: String, o: Outcome): Unit = {
    println(s"DIAG $tag lingsLeft=${o.lingsLeft} sunkensLeft=${o.sunkensLeft} goonsLeft=${o.goonsLeft}/5 " +
      s"reaver=${o.reaverHp}/${Protoss.Reaver.maxTotalHealth.toInt} shots=${o.shots} frames=${o.frames}")
  }

  private def packedLings: Seq[Body] =
    (0 until 25).map(i =>
      new Body(Zerg.Zergling, (i % 5) * 30.0 - 60.0, 650.0 + (i / 5) * 30.0, Zerg.Zergling.maxTotalHealth))

  private def columnLings: Seq[Body] =
    (0 until 25).map(i =>
      new Body(Zerg.Zergling, 0.0, 200.0 + i * 40.0, Zerg.Zergling.maxTotalHealth))

  test("map scale: reaver plus five dragoons vs 25 lings screening three sunkens") {
    val o = runScenario(packedLings, None)
    report("scale", o)
    assert(o.lingsLeft == 0 && o.sunkensLeft == 0, "shape must clear the map with ammo to spare")
    assert(o.shots <= 10, s"winning line must stay near the live budget, cost ${o.shots}")
  }

  test("spread screen: the same escort against lings arriving in a column") {
    val o = runScenario(columnLings, None)
    report("column", o)
    assert(o.lingsLeft == 0 && o.sunkensLeft == 0, "shape must clear the spread screen too")
    assert(o.shots <= 12, s"spread screen must stay near the live budget, cost ${o.shots}")
  }

  test("scarce hull: five scarabs against the packed screen and three sunkens") {
    val o = runScenario(packedLings, Some(new Ammo(5)), kite = false)
    report("capped5", o)
    assert(o.lingsLeft == 0, "the screen must still clear - escort-first holds")
    assert(o.sunkensLeft >= 1, "five scarabs cannot finish three sunkens: the wall")
  }

  test("kiting escort: the same five dragoons behind the shipped kite kernel") {
    val o = runScenario(packedLings, Some(new Ammo(5)), kite = true)
    report("kite", o)
    assert(o.sunkensLeft >= 1, "the shipped kite kernel does not move the wall")
  }
}
