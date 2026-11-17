package starengine

import Micro.Actions.Protoss.Zealot.ZealotLeash
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Zealot leash, step by step: a Zealot duels three Dragoons while our own
 * Dragoons fall back out of leash range. The pilot calls the shipped
 * ZealotLeash kernel on the same facts production reads (nearest own
 * Dragoon in tiles) and rejoins the line instead of charging alone -
 * scouts, committed rushers and suicides keep their explicit orders and
 * are not piloted here. Speeds, reaches and damage are the real UnitType
 * numbers.
 */
class ZealotLeashScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Zealot.name + StarEngineTags.Cohesion.name) }

  /** Pilot, production order: the shipped kernel owns the call, legs walk. */
  private class LeashPilot(dragoons: Seq[Body]) extends Pilot {
    var rejoins = 0
    var duels = 0

    override def direct(me: Body, world: World): Directive = {
      val nearest = dragoons.filter(_.alive).sortBy(me.dist).headOption
      val tiles = nearest.map(me.dist(_) / 32.0).getOrElse(Double.PositiveInfinity)
      if (ZealotLeash.shouldRejoin(tiles)) {
        rejoins += 1
        nearest.map(n => MoveTo(n.x, n.y)).getOrElse(Hold)
      } else {
        duels += 1
        world.foes.filter(_.alive).sortBy(me.dist).headOption.map(Chase).getOrElse(Hold)
      }
    }
  }

  test("a lone Zealot rejoins its Dragoons past five and a half tiles") {
    val zealot = new Body(Protoss.Zealot, 0.0, 500.0, Protoss.Zealot.maxTotalHealth)
    val dragoons = Seq(
      new Body(Protoss.Dragoon, 300.0, 500.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 340.0, 500.0, Protoss.Dragoon.maxTotalHealth))
    val foes = (0 until 3).map(i =>
      new Body(Protoss.Dragoon, 150.0 + i * 30.0, 560.0, Protoss.Dragoon.maxTotalHealth)).toSeq
    val pilot = new LeashPilot(dragoons)
    val world = new World(Seq(zealot) ++ dragoons, foes,
      Map(zealot -> pilot) ++ dragoons.map(_ -> new Pilots.Hold).toMap ++ foes.map(_ -> new Pilots.Hold).toMap, Seq.empty)

    var f = 0
    var startDist = -1.0
    while (f < 600 && zealot.alive) {
      if (f == 0) startDist = zealot.dist(dragoons.head)
      world.step()
      f = world.frame
    }
    println(s"DIAG leash rejoins=${pilot.rejoins} duels=${pilot.duels} start=$startDist end=${zealot.dist(dragoons.head)}")
    assert(pilot.rejoins > 0, "leash never fired past 5.5 tiles")
    assert(zealot.dist(dragoons.head) < startDist, "zealot never closed with its Dragoons")
  }

  test("inside the leash the Zealot duels instead of walking home") {
    val zealot = new Body(Protoss.Zealot, 0.0, 500.0, Protoss.Zealot.maxTotalHealth)
    val dragoons = Seq(new Body(Protoss.Dragoon, 100.0, 500.0, Protoss.Dragoon.maxTotalHealth))
    val foe = new Body(Protoss.Dragoon, 150.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val pilot = new LeashPilot(dragoons)
    val world = new World(Seq(zealot) ++ dragoons, Seq(foe),
      Map(zealot -> pilot, dragoons.head -> new Pilots.Hold, foe -> new Pilots.Hold), Seq.empty)

    var f = 0
    while (f < 300 && zealot.alive && foe.alive) {
      world.step()
      f = world.frame
    }
    println(s"DIAG unleashed rejoins=${pilot.rejoins} duels=${pilot.duels} foe=${foe.hp}")
    assert(pilot.rejoins == 0, "leash fired inside 5.5 tiles")
    assert(pilot.duels > 0, "zealot never dueled")
  }
}
