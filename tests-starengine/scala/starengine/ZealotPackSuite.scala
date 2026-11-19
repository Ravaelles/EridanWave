package starengine

import Micro.Actions.Combat.Decisioning.ZealotPack
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Zealot pack, step by step: a lone Zealot of a small zealots-only
 * force faces a Marine downfield. The shipped ZealotPack gate owns
 * the answer - no packmate inside one tile, no approach. The hull
 * holds the pack centroid instead of charging alone; once a mate
 * joins inside a tile both hulls close and kill. Speeds, reaches and
 * damage are the real UnitType numbers; the gate is the shipped
 * kernel, the pilots only execute it.
 */
class ZealotPackSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Zealot.name + StarEngineTags.Cohesion.name) }

  private class PackPilot(centroid: (Double, Double)) extends Pilot {

    override def direct(me: Body, world: World): Directive = {
      val mates = world.ours.filter(o => o.alive && o.kind == Protoss.Zealot && (o ne me))
      val mateTiles = mates.sortBy(me.dist).headOption.map(me.dist(_) / 32.0).getOrElse(Double.PositiveInfinity)
      val ours = world.ours.count(o => o.alive && o.kind == Protoss.Zealot)
      val inContact = world.foes.exists(f => f.alive && me.dist(f) <= me.rangeAgainst(f) + 32.0)
      val regroup = ZealotPack.shouldRegroup(
        onlyZealots = world.ours.forall(o => ! o.alive || o.kind == Protoss.Zealot),
        zealots = ours,
        mateTiles = mateTiles,
        usSkim = 100.0,
        foeSkim = 100.0,
        inContact = inContact)
      if (regroup) MoveTo(centroid._1, centroid._2)
      else world.foes.filter(_.alive).sortBy(me.dist).headOption.map(Chase).getOrElse(Hold)
    }
  }

  test("a lone hull holds the pack instead of charging alone") {
    val zealot = new Body(Protoss.Zealot, 0.0, 500.0, Protoss.Zealot.maxTotalHealth)
    val marine = new Body(Terran.Marine, 300.0, 500.0, Terran.Marine.maxTotalHealth)
    val world = new World(Seq(zealot), Seq(marine),
      Map(zealot -> new PackPilot((0.0, 500.0)), marine -> new Pilots.Hold), Seq.empty)

    while (world.frame < 2000 && marine.alive) {
      world.step()
    }

    println(s"DIAG lone frames=${world.frame} zealot=${zealot.x.toInt} marine=${marine.hp.toInt}")
    assert(marine.alive, "lone hull charged instead of holding the pack")
    assert(Math.abs(zealot.x - 0.0) <= 100.0, f"lone hull left the centroid to ${zealot.x.toInt}px")
  }

  test("a joined mate releases the approach") {
    val first = new Body(Protoss.Zealot, 0.0, 500.0, Protoss.Zealot.maxTotalHealth)
    val second = new Body(Protoss.Zealot, 32.0, 500.0, Protoss.Zealot.maxTotalHealth)
    val marine = new Body(Terran.Marine, 300.0, 500.0, Terran.Marine.maxTotalHealth)
    val world = new World(Seq(first, second), Seq(marine),
      Map(first -> new PackPilot((0.0, 500.0)), second -> new PackPilot((0.0, 500.0)), marine -> new Pilots.Hold), Seq.empty)

    while (world.frame < 3000 && marine.alive && (first.alive || second.alive)) {
      world.step()
    }

    println(s"DIAG pair frames=${world.frame} marine=${marine.hp.toInt} hulls=${first.hp.toInt},${second.hp.toInt}")
    assert( ! marine.alive, "joined ball never closed the marine")
  }
}
