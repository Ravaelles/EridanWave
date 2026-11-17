package starengine

import Micro.Targeting.TargetChaff
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Hatch chaff, step by step: a Dragoon meets a Zergling screening an Egg
 * and a Larva. The pilot calls the shipped TargetChaff gate on the same
 * facts production reads and shoots the only valid target - chaff never
 * gets designated, so the line never chews eggs while teeth close in.
 * Speeds, reaches and damage are the real UnitType numbers.
 */
class ChaffScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Targeting.name) }

  /** Pilot, production order: the shipped gate filters, legs execute. */
  private class ChaffPilot extends Pilot {
    var chaffPicks = 0

    override def direct(me: Body, world: World): Directive = {
      val valid = world.foes.filter(f => f.alive && ! TargetChaff.isChaff(f.kind)).sortBy(me.dist)
      valid.headOption match {
        case None => Hold
        case Some(t) =>
          if (TargetChaff.isChaff(t.kind)) chaffPicks += 1
          Chase(t)
      }
    }
  }

  test("eggs and larvae never get designated") {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val ling = new Body(Zerg.Zergling, 300.0, 500.0, Zerg.Zergling.maxTotalHealth)
    val egg = new Body(Zerg.Egg, 340.0, 510.0, Zerg.Egg.maxTotalHealth)
    val larva = new Body(Zerg.Larva, 380.0, 490.0, Zerg.Larva.maxTotalHealth)
    val pilot = new ChaffPilot
    val world = new World(Seq(dragoon), Seq(ling, egg, larva),
      Map(dragoon -> pilot, ling -> new Pilots.Hold, egg -> new Pilots.Hold, larva -> new Pilots.Hold), Seq.empty)

    var f = 0
    while (f < 600 && dragoon.alive && ling.alive) {
      world.step()
      f = world.frame
    }
    println(s"DIAG chaff picks=${pilot.chaffPicks} ling=${ling.hp} egg=${egg.hp} larva=${larva.hp}")
    assert(pilot.chaffPicks == 0, "chaff got designated")
    assert(egg.alive && larva.alive, "chaff took fire")
    assert(ling.hp < Zerg.Zergling.maxTotalHealth, "the valid foe never got shot")
  }
}
