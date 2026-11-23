package starengine

import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable

/**
 * ZealDrag opening (retired stand): the 48-frame freeze masked the line
 * never forming, so it was removed - one solution for UMS and ladder
 * alike. The pilot below chases from frame zero; the suite pins that no
 * freeze returns: hulls move inside the old window and close the gap.
 * Ground units only - the observer rides along but never counts.
 */
class ZealDragOpeningSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.March.name + StarEngineTags.Formation.name + StarEngineTags.Dragoon.name + StarEngineTags.Zealot.name + StarEngineTags.PvP.name) }

  /** No freeze: chase from frame zero, the stand is retired. Ground only here. */
  private class StandPilot(ids: Map[Body, Int]) extends Pilot {
    val directives: mutable.ListBuffer[(Int, Int, String)] = mutable.ListBuffer.empty

    override def direct(me: Body, world: World): Directive = {
      // Eyes never count and never chase: hold aside, out of the way.
      if (me.kind == Protoss.Observer) return Hold

      val foe = world.nearestFoeOf(me, world.foes, (_, _) => true)
      val d = foe.map(Chase).getOrElse(Hold)
      directives.append((world.frame, ids(me), d.getClass.getSimpleName))
      d
    }
  }

  private case class Opening(
    movedBy47: Map[Int, Double],
    heldAt36: Boolean,
    avgDist0: Double,
    avgDist47: Double,
    avgDistEnd: Double,
    allyGap0: Double,
    allyGap47: Double)

  private def avgDist(ours: Seq[Body], foes: Seq[Body]): Double = {
    val liveFoes = foes.filter(_.alive)
    if (liveFoes.isEmpty) return 0.0

    val ds = ours.filter(o => o.alive && ! o.kind.isFlyer).map(o => liveFoes.map(o.dist).min)
    if (ds.isEmpty) 0.0 else ds.sum / ds.size
  }

  private def allyGap(ours: Seq[Body]): Double = {
    val live = ours.filter(o => o.alive && ! o.kind.isFlyer)
    if (live.size < 2) 0.0
    else live.map(u => live.filterNot(_ eq u).map(u.dist).min).max
  }

  private def runOpening(): (Opening, Seq[Body], Seq[Body]) = {
    // Spawn-like scatter: two blades and two guns apart, observer aside.
    val z1 = new Body(Protoss.Zealot, -200.0, 100.0, Protoss.Zealot.maxTotalHealth)
    val z2 = new Body(Protoss.Zealot, 200.0, -100.0, Protoss.Zealot.maxTotalHealth)
    val d1 = new Body(Protoss.Dragoon, -150.0, -150.0, Protoss.Dragoon.maxTotalHealth)
    val d2 = new Body(Protoss.Dragoon, 150.0, 150.0, Protoss.Dragoon.maxTotalHealth)
    val obs = new Body(Protoss.Observer, 0.0, 400.0, Protoss.Observer.maxTotalHealth)
    val ours = Seq(z1, z2, d1, d2, obs)
    val spawn = ours.map(b => b -> (b.x, b.y)).toMap
    val foes = Seq(
      new Body(Protoss.Zealot, -200.0, -860.0, Protoss.Zealot.maxTotalHealth),
      new Body(Protoss.Zealot, 200.0, -1060.0, Protoss.Zealot.maxTotalHealth),
      new Body(Protoss.Dragoon, -150.0, -1110.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 150.0, -910.0, Protoss.Dragoon.maxTotalHealth))
    val ids = ours.zipWithIndex.map { case (b, i) => b -> (101 + i) }.toMap
    val pilot = new StandPilot(ids)
    val world = new World(
      ours, foes,
      (ours.map(_ -> pilot) ++ foes.map(_ -> new Pilots.ZombieAttack(_.ours, (0.0, -960.0)))).toMap,
      Seq.empty)
    val warriors = Seq(z1, z2, d1, d2)
    var avg0 = avgDist(ours, foes)
    var avg47 = 0.0
    var gap0 = allyGap(ours)
    var gap47 = 0.0
    val disp = mutable.HashMap[Int, Double]()
    while (world.frame < 200 && foes.exists(_.alive) && ours.exists(_.alive)) {
      world.step()
      val f = world.frame
      if (f == 47) {
        avg47 = avgDist(ours, foes)
        gap47 = allyGap(ours)
        warriors.foreach(u => {
          val (sx, sy) = spawn(u)
          disp(ids(u)) = Math.hypot(u.x - sx, u.y - sy)
        })
      }
    }
    val warriorIds = warriors.map(ids).toSet
    val held36 = pilot.directives.filter(d => d._1 == 36 && warriorIds.contains(d._2)).forall(_._3 == "Chase")
    val avgEnd = avgDist(ours, foes)
    (Opening(disp.toMap, held36, avg0, avg47, avgEnd, gap0, gap47), ours, foes)
  }

  test("no opening freeze: the line moves inside the old window and closes") {
    val (m, ours, foes) = runOpening()
    println("DIAG moved=" + m.movedBy47.values.map(_.toInt).mkString(",") +
      " chasing36=" + m.heldAt36 +
      " avg=" + m.avgDist0.toInt + "->" + m.avgDist47.toInt + "->" + m.avgDistEnd.toInt +
      " gap=" + m.allyGap0.toInt + "->" + m.allyGap47.toInt)
    m.movedBy47.foreach { case (id, px) =>
      assert(px > 0.0, f"hull $id stood frozen inside the old window")
    }
    assert(m.heldAt36, "the line was not chasing at 1.5 seconds")
    assert(m.avgDistEnd < m.avgDist0, "the line never closed after spawn")
  }
}
