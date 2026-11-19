package starengine

import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

class MirrorPlantSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Zealot.name + StarEngineTags.Targeting.name + StarEngineTags.PvP.name) }

  /** Milling means chasing nearest and never stopping. */
  private def millPilot(fallback: (Double, Double)): Pilot =
    new Pilots.AttackMove(_.foes, (_, _) => true, fallback)

  /** Planted means holding inside gun range on the shared weakest. */
  private class PlantPilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val live = if (world.ours.contains(me)) world.foes else world.ours
      val inRange = live.filter(f => f.alive && me.dist(f) <= me.rangeAgainst(f))
      if (inRange.nonEmpty) Hold
      else live.filter(_.alive).sortBy(_.hp).headOption.map(Chase).getOrElse(Hold)
    }
  }

  test("planted focus beats milling: hold in range, kill weakest") {
    val ours = (-1 to 1).map(i => new Body(Protoss.Zealot, i * 40.0, 900.0, Protoss.Zealot.maxTotalHealth))
    val foes = (-1 to 1).map(i => new Body(Protoss.Zealot, i * 40.0, 100.0, Protoss.Zealot.maxTotalHealth))
    val w1 = new World(ours, foes, (ours.map(_ -> new PlantPilot) ++ foes.map(_ -> millPilot((0.0, 900.0)))).toMap, Seq.empty)
    w1.run(6000)
    val plantKills = 3 - foes.count(_.alive)
    val millKillsA = 3 - ours.count(_.alive)
    val ours2 = (-1 to 1).map(i => new Body(Protoss.Zealot, i * 40.0, 900.0, Protoss.Zealot.maxTotalHealth))
    val foes2 = (-1 to 1).map(i => new Body(Protoss.Zealot, i * 40.0, 100.0, Protoss.Zealot.maxTotalHealth))
    val w2 = new World(ours2, foes2, (ours2.map(_ -> millPilot((0.0, 100.0))) ++ foes2.map(_ -> new PlantPilot)).toMap, Seq.empty)
    w2.run(6000)
    val millKills = 3 - foes2.count(_.alive)
    val plantKillsB = 3 - ours2.count(_.alive)
    println(s"DIAG plant-vs-mill plantKills=$plantKills millKillsA=$millKillsA; mill-vs-plant millKills=$millKills plantKillsB=$plantKillsB")
    assert(plantKills + plantKillsB > millKills + millKillsA, "planting must out-kill milling across both seats")
  }
}
