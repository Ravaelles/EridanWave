package starengine

import ProxyBwapi.Races.Protoss
import Tactic.Squads.Decisions.ClusterCohesion
import org.scalatest.funsuite.AnyFunSuite

/**
 * Cluster cohesion, step by step: a Dragoon duels far ahead while its
 * pack holds the line fourteen tiles back. The pilot calls the shipped
 * ClusterCohesion kernel on the same facts production reads (warrior,
 * no victim, no retreat, in battle, macro game, no local contact,
 * distance to the pack centroid) and walks home instead of dueling
 * alone - a hull with enemies on it keeps its fight, a marching column
 * keeps its march. Speeds, reaches and damage are the real UnitType
 * numbers.
 */
class ClusterRejoinScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Cohesion.name + StarEngineTags.Dragoon.name) }

  /** Pilot, production order: the shipped kernel owns the call, legs walk. */
  private class RejoinPilot(centroid: (Double, Double), inBattle: Boolean, var target: Option[Body]) extends Pilot {
    var rejoins = 0
    var duels = 0

    override def direct(me: Body, world: World): Directive = {
      val foes = world.foes.filter(_.alive)
      val nearestFoe = foes.sortBy(me.dist).headOption
      val contact = nearestFoe.exists(f => me.dist(f) <= me.rangeAgainst(f) + 2 * 32.0)
      val distTiles = {
        val dx = me.x - centroid._1
        val dy = me.y - centroid._2
        Math.sqrt(dx * dx + dy * dy) / 32.0
      }
      val hasTarget = target.exists(_.alive)
      if (ClusterCohesion.shouldRejoin(
        isWarrior = true,
        hasTarget = hasTarget,
        retreating = false,
        coolingUnderFire = false,
        inBattle = inBattle,
        macroGame = true,
        hasLocalContact = contact,
        distTiles = distTiles)) {
        rejoins += 1
        MoveTo(centroid._1, centroid._2)
      } else {
        duels += 1
        foes.sortBy(me.dist).headOption.map(Chase).getOrElse(Hold)
      }
    }
  }

  test("a drifter past fourteen tiles walks home instead of dueling") {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val pack = Seq(
      new Body(Protoss.Dragoon, 600.0, 500.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 640.0, 500.0, Protoss.Dragoon.maxTotalHealth))
    val foe = new Body(Protoss.Dragoon, 900.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val centroid = (620.0, 500.0)
    val pilot = new RejoinPilot(centroid, inBattle = true, target = None)
    val world = new World(Seq(dragoon) ++ pack, Seq(foe),
      Map(dragoon -> pilot) ++ pack.map(_ -> new Pilots.Hold).toMap ++ Map(foe -> new Pilots.Hold), Seq.empty)

    var f = 0
    val startDist = Math.abs(dragoon.x - centroid._1)
    while (f < 600 && dragoon.alive) {
      world.step()
      f = world.frame
    }
    val endDist = Math.abs(dragoon.x - centroid._1)
    println(s"DIAG drift rejoins=${pilot.rejoins} start=$startDist end=$endDist")
    assert(pilot.rejoins > 0, "drifter never turned home")
    assert(endDist < startDist, "drifter never closed with its cluster")
  }

  test("a hull with contact keeps its fight and a column keeps its march") {
    val dragoon = new Body(Protoss.Dragoon, 500.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val pack = Seq(new Body(Protoss.Dragoon, 1000.0, 500.0, Protoss.Dragoon.maxTotalHealth))
    val foe = new Body(Protoss.Dragoon, 560.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val centroid = (1000.0, 500.0)
    val pilot = new RejoinPilot(centroid, inBattle = true, target = Some(foe))
    val world = new World(Seq(dragoon), Seq(foe),
      Map(dragoon -> pilot, foe -> new Pilots.Hold), Seq.empty)

    var f = 0
    while (f < 300 && dragoon.alive && foe.alive) {
      world.step()
      f = world.frame
    }
    println(s"DIAG contact rejoins=${pilot.rejoins} duels=${pilot.duels} foe=${foe.hp}")
    assert(pilot.rejoins == 0, "contact hull got reeled in")
    assert(pilot.duels > 0, "contact hull never dueled")
  }
}
