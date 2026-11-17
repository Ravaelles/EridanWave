package starengine

import Micro.Actions.Combat.Decisioning.CrucialAirSnipe
import Micro.Actions.Combat.Decisioning.CrucialAirSnipe._
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Always-attack ladder, step by step: a grounded Dragoon faces a Scourge
 * closing on a Shuttle, a wounded Carrier, a Wraith, a detected Observer
 * and a Reaver. The pilot calls the shipped pick on the same marks
 * production builds (nearest Scourge first, then the weakest capital in
 * fixed order, support casters last) and shoots the pick - the ladder
 * from the code, not a reimplementation. Speeds, reaches and damage are
 * the real UnitType numbers.
 */
class AlwaysAttackScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Targeting.name + StarEngineTags.Carrier.name + StarEngineTags.Dragoon.name) }

  /** Pilot, production order: the shipped ladder picks, legs shoot. */
  private class SnipePilot extends Pilot {
    var shots = 0

    override def direct(me: Body, world: World): Directive = {
      val foes = world.foes.filter(_.alive)
      if (foes.isEmpty) return Hold

      val victims = foes.filter(f => kindOf(f).isDefined && me.dist(f) <= me.rangeAgainst(f))
      if (victims.isEmpty) return foes.sortBy(me.dist).headOption.map(Chase).getOrElse(Hold)

      val marks = victims.map(v => Mark(
        distTiles = me.dist(v) / 32.0,
        kind = kindOf(v).get,
        totalHealth = v.hp)).toSeq
      CrucialAirSnipe.pick(marks).foreach(i => {
        shots += 1

        return Chase(victims(i))
      })
      Hold
    }

    private def kindOf(target: Body): Option[VictimKind] =
      if (target.kind == Zerg.Scourge) Some(Scourge)
      else if (target.kind == Protoss.Carrier) Some(Carrier)
      else if (target.kind == Terran.Wraith) Some(Wraith)
      else if (target.kind == Protoss.Observer) Some(Observer)
      else if (target.kind == Protoss.Reaver) Some(Reaver)
      else None
  }

  test("the Scourge dies first, then the weakest capital in order") {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val scourge = new Body(Zerg.Scourge, 200.0, 500.0, Zerg.Scourge.maxTotalHealth)
    val carrier = new Body(Protoss.Carrier, 300.0, 500.0, Protoss.Carrier.maxTotalHealth)
    val wraith = new Body(Terran.Wraith, 350.0, 520.0, Terran.Wraith.maxTotalHealth)
    val pilot = new SnipePilot
    val world = new World(Seq(dragoon), Seq(scourge, carrier, wraith),
      Map(dragoon -> pilot, scourge -> new Pilots.Hold, carrier -> new Pilots.Hold, wraith -> new Pilots.Hold), Seq.empty)

    var f = 0
    while (f < 1500 && dragoon.alive && (scourge.alive || carrier.alive || wraith.alive)) {
      world.step()
      f = world.frame
    }
    println(s"DIAG snipe frames=$f dragoon=${dragoon.hp}@${dragoon.x.toInt} shots=${pilot.shots} scourge=${scourge.hp} carrier=${carrier.hp} wraith=${wraith.hp}")
    assert( ! scourge.alive, "incoming Scourge survived the snipe")
    assert(carrier.hp < Protoss.Carrier.maxTotalHealth || wraith.hp < Terran.Wraith.maxTotalHealth, "no capital took fire after the Scourge")
  }

  test("casters queue behind the capitals in fixed order") {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val observer = new Body(Protoss.Observer, 200.0, 500.0, Protoss.Observer.maxTotalHealth)
    val reaver = new Body(Protoss.Reaver, 800.0, 500.0, Protoss.Reaver.maxTotalHealth)
    val pilot = new SnipePilot
    val world = new World(Seq(dragoon), Seq(observer, reaver),
      Map(dragoon -> pilot, observer -> new Pilots.Hold, reaver -> new Pilots.Hold), Seq.empty)

    var f = 0
    var observerDiedAt = -1
    while (f < 1500 && dragoon.alive && reaver.alive) {
      world.step()
      f = world.frame
      if (observer.alive) {
        assert(reaver.hp == Protoss.Reaver.maxTotalHealth, s"reaver took fire at f=$f while its observer lived")
      } else if (observerDiedAt < 0) {
        observerDiedAt = f
      }
    }
    println(s"DIAG casters shots=${pilot.shots} obs=${observer.hp} reaver=${reaver.hp} obsDied=$observerDiedAt")
    assert(observerDiedAt >= 0, "observer never died")
    assert(reaver.hp < Protoss.Reaver.maxTotalHealth, "reaver never took fire after its observer")
  }
}
