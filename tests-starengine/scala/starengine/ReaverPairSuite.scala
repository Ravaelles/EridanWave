package starengine

import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Reaver pair grind, three geometries (close, mid with Sunkens, far):
 * the Reaver holds its anchor and kills everything with splash while
 * Dragoon and Zealot decoys step back, never fire (passive) and absorb
 * the contact. Hydralisks and Zerglings always press (zombie); the
 * Shuttle holds far behind, untouched. The bar from the field: the
 * Reaver and the ride both finish above 90% hull.
 */
class ReaverPairSuite extends AnyFunSuite {

  /** Reaver discipline: shoot from the anchor, never chase; walk statics down to max range minus a margin. */
  private class StandPilot(anchor: (Double, Double)) extends Pilot {
    override def direct(me: Body, world: World): Directive = {
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

  /** Decoy discipline: passive (never shoots), steps back to its fallback
    * under pressure and holds there absorbing contact, walks home when clear. */
  private class DecoyBack(anchor: (Double, Double), fallback: (Double, Double), staging: (Double, Double), releaseFrame: Int) extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      if (world.frame < releaseFrame) return MoveTo(staging._1, staging._2)

      world.nearestFoeOf(me, world.foes, (_, _) => true) match {
        case Some(n) if me.dist(n) <= 256.0 => MoveTo(fallback._1, fallback._2)
        case _ => MoveTo(anchor._1, anchor._2)
      }
    }
  }

  private case class Outcome(hydrasLeft: Int, lingsLeft: Int, staticsLeft: Int, reaverHp: Int, shuttleHp: Int, frames: Int)

  private def runPack(
    foes: Seq[Body],
    decoys: Seq[(Body, Int)],
    reaverPos: (Double, Double),
    shuttlePos: (Double, Double)): Outcome = {
    val reaver = new Body(Protoss.Reaver, reaverPos._1, reaverPos._2, Protoss.Reaver.maxTotalHealth)
    val shuttle = new Body(Protoss.Shuttle, shuttlePos._1, shuttlePos._2, Protoss.Shuttle.maxTotalHealth)
    decoys.foreach { case (d, _) => d.passive = true }
    val pilots: Map[Body, Pilot] = Map(
      reaver -> new StandPilot(reaverPos),
      shuttle -> new Pilots.Hold) ++
      decoys.map { case (d, r) => d -> new DecoyBack((d.x, 650.0), (d.x, 690.0), (d.x, d.y), r) }.toMap ++
      foes.collect { case h if h.kind == Zerg.Hydralisk => h -> new Pilots.ZombieAttack(_.ours, (h.x, h.y)) }.toMap ++
      foes.collect { case l if l.kind == Zerg.Zergling => l -> new Pilots.ZombieAttack(_.ours, (l.x, l.y)) }.toMap ++
      foes.collect { case s if s.kind.isBuilding => s -> new Pilots.Hold }.toMap
    val world = new World(Seq(reaver, shuttle) ++ decoys.map(_._1), foes, pilots, Seq.empty)

    while (world.frame < 4000 && foes.exists(_.alive) && reaver.alive) {
      world.step()
    }
    Outcome(
      hydrasLeft = foes.count(f => f.alive && f.kind == Zerg.Hydralisk),
      lingsLeft = foes.count(f => f.alive && f.kind == Zerg.Zergling),
      staticsLeft = foes.count(f => f.alive && f.kind.isBuilding),
      reaverHp = reaver.hp.toInt,
      shuttleHp = shuttle.hp.toInt,
      frames = world.frame)
  }

  private def check(o: Outcome, tag: String): Unit = {
    println(s"DIAG $tag hydras=${o.hydrasLeft} lings=${o.lingsLeft} statics=${o.staticsLeft} " +
      s"reaver=${o.reaverHp} shuttle=${o.shuttleHp} frames=${o.frames}")
    assert(o.hydrasLeft == 0 && o.lingsLeft == 0 && o.staticsLeft == 0, "reaver did not kill everything")
    assert(o.reaverHp >= 0.9 * Protoss.Reaver.maxTotalHealth, "reaver finished below 90%")
    assert(o.shuttleHp >= 0.9 * Protoss.Shuttle.maxTotalHealth, "shuttle finished below 90%")
  }

  test("close pack, no statics: the reaver mulches it from the anchor") {
    val foes = (0 until 4).map(i => new Body(Zerg.Hydralisk, -60.0 + (i % 2) * 120.0, 300.0 + (i / 2) * 40.0, Zerg.Hydralisk.maxTotalHealth)) ++
      (0 until 4).map(i => new Body(Zerg.Zergling, -90.0 + (i % 2) * 180.0, 340.0 + (i / 2) * 40.0, Zerg.Zergling.maxTotalHealth))
    val decoys = Seq(
      new Body(Protoss.Dragoon, -80.0, 650.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 80.0, 650.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Zealot, 0.0, 640.0, Protoss.Zealot.maxTotalHealth)).map(_ -> 0)
    check(runPack(foes, decoys, (0.0, 700.0), (0.0, 1100.0)), "close")
  }

  test("mid pack under two sunkens: escort-first, then the statics from max range") {
    val foes = Seq(
      new Body(Zerg.SunkenColony, -150.0, 0.0, Zerg.SunkenColony.maxTotalHealth),
      new Body(Zerg.SunkenColony, 150.0, 0.0, Zerg.SunkenColony.maxTotalHealth)) ++
      (0 until 4).map(i => new Body(Zerg.Hydralisk, -60.0 + (i % 2) * 120.0, 250.0 + (i / 2) * 40.0, Zerg.Hydralisk.maxTotalHealth)) ++
      (0 until 4).map(i => new Body(Zerg.Zergling, -90.0 + (i % 2) * 180.0, 290.0 + (i / 2) * 40.0, Zerg.Zergling.maxTotalHealth))
    val decoys = Seq(
      new Body(Protoss.Dragoon, -80.0, 650.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 80.0, 650.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Zealot, 0.0, 640.0, Protoss.Zealot.maxTotalHealth),
      new Body(Protoss.Dragoon, 140.0, 660.0, Protoss.Dragoon.maxTotalHealth)).zip(Seq(0, 0, 0, 400))
    check(runPack(foes, decoys, (0.0, 700.0), (0.0, 1100.0)), "sunkens")
  }

  test("far hydra army, no statics: the line absorbs while the reaver grinds") {
    val foes = (0 until 5).map(i => new Body(Zerg.Hydralisk, -100.0 + (i % 3) * 100.0, 100.0 + (i / 3) * 40.0, Zerg.Hydralisk.maxTotalHealth)) ++
      (0 until 4).map(i => new Body(Zerg.Zergling, -75.0 + (i % 2) * 150.0, 140.0 + (i / 2) * 40.0, Zerg.Zergling.maxTotalHealth))
    val decoys = Seq(
      new Body(Protoss.Dragoon, -80.0, 650.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Zealot, 0.0, 640.0, Protoss.Zealot.maxTotalHealth),
      new Body(Protoss.Zealot, 90.0, 640.0, Protoss.Zealot.maxTotalHealth),
      new Body(Protoss.Dragoon, 80.0, 650.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, -80.0, 1150.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 80.0, 1150.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Zealot, 0.0, 1150.0, Protoss.Zealot.maxTotalHealth),
      new Body(Protoss.Zealot, 90.0, 1150.0, Protoss.Zealot.maxTotalHealth)).zip(Seq(0, 0, 0, 0, 150, 300, 450, 600))
    check(runPack(foes, decoys, (0.0, 700.0), (0.0, 1100.0)), "far")
  }
}
