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

  test("a mutual approach meets in the middle: closing enemies are contact", StarEngineTags.PvP) {
    // The 3Zeal wall: packs closing on each other never regroup home -
    // a foe warrior whose distance shrinks is an incoming fight, not
    // a lone charge. The pilot measures closing the way production
    // does (presumptive target or approach speed): shrinking distance.
    val ours = (-1 to 1).map(i => new Body(Protoss.Zealot, i * 100.0, 900.0, Protoss.Zealot.maxTotalHealth))
    val foes = (-1 to 1).map(i => new Body(Protoss.Zealot, i * 100.0, 100.0, Protoss.Zealot.maxTotalHealth))
    var regroups = 0
    val pilots: Map[Body, Pilot] = (ours.map(o => o -> new Pilot {
      override def direct(me: Body, world: World): Directive = {
        val live = world.foes.filter(_.alive)
        // Intent, not geometry: a foe ordered onto one of ours is
        // closing even at equal speed, where distance trends stall.
        val closing = live.filter(_.kind == Protoss.Zealot).exists(f =>
          world.orderOf(f).exists({
            case Chase(t) => t.alive && world.ours.contains(t)
            case _        => false
          }))
        val mates = world.ours.filter(x => x.alive && (x ne me))
        val mateTiles = mates.sortBy(me.dist).headOption.map(me.dist(_) / 32.0).getOrElse(Double.PositiveInfinity)
        val regroup = ZealotPack.shouldRegroup(
          onlyZealots = true,
          zealots = world.ours.count(_.alive),
          mateTiles = mateTiles,
          usSkim = 100.0,
          foeSkim = 100.0,
          inContact = live.exists(f => me.dist(f) <= me.rangeAgainst(f) + 32.0),
          enemyClosing = closing)
        if (regroup) {
          regroups += 1
          MoveTo(0.0, 1200.0)
        }
        else live.sortBy(me.dist).headOption.map(Chase).getOrElse(Hold)
      }
    }) ++ foes.map(f => f -> new Pilots.ZombieAttack(_.ours, (0.0, 900.0)))).toMap
    val world = new World(ours, foes, pilots, Seq.empty)
    world.run(3000)
    val foeHp = foes.map(_.hp.toInt).sum
    println(s"DIAG mirror regroups=$regroups ours=${ours.count(_.alive)}/3 foeHp=$foeHp frames=${world.frame}")
    // Foes shoot first in this engine, so a symmetric grind favors them -
    // the mechanism under test is the meeting, not the winning: no
    // regroup yo-yo past the opening frames, and the packs bleed.
    assert(regroups <= 6, "mutual approach regrouped home instead of meeting")
    assert(foeHp < 3 * Protoss.Zealot.maxTotalHealth.toInt, "packs never joined")
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
