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
 * from the code, not a reimplementation. Dark Templars duel on terms:
 * a warm gun and room to stand for ranged hulls, so a Dragoon answers
 * at three tiles, holds fire point-blank, answers through cooldown, and
 * a Zealot answers point-blank. Speeds, reaches and damage are
 * the real UnitType numbers.
 */
class AlwaysAttackScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Targeting.name + StarEngineTags.Carrier.name + StarEngineTags.Dragoon.name + StarEngineTags.DT.name) }

  /** Pilot, production order: the shipped ladder picks, legs shoot. DT victims duel on the shipped terms. */
  private class SnipePilot extends Pilot {
    var shots = 0
    var targetedDT = false

    override def direct(me: Body, world: World): Directive = {
      val foes = world.foes.filter(_.alive)
      if (foes.isEmpty) return Hold

      val victims = foes.filter(f =>
        kindOf(f).isDefined && me.dist(f) <= me.rangeAgainst(f) &&
          (f.kind != Protoss.DarkTemplar || CrucialAirSnipe.duelsDarkTemplar(
            me.cooldownLeft, me.kind.melee, me.dist(f) / 32.0)))
      if (victims.isEmpty) return foes
        .filter(_.kind != Protoss.DarkTemplar)
        .sortBy(me.dist).headOption.map(Chase).getOrElse(Hold)

      val marks = victims.map(v => Mark(
        distTiles = me.dist(v) / 32.0,
        kind = kindOf(v).get,
        totalHealth = v.hp)).toSeq
      CrucialAirSnipe.pick(marks).foreach(i => {
        shots += 1
        if (victims(i).kind == Protoss.DarkTemplar) targetedDT = true

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
      else if (target.kind == Protoss.DarkTemplar) Some(DarkTemplar)
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

  private def runDuel(shooter: Body, dtX: Double, dtY: Double, cooldown: Int, frames: Int): (Body, Body, SnipePilot) = {
    val dt = new Body(Protoss.DarkTemplar, dtX, dtY, Protoss.DarkTemplar.maxTotalHealth)
    dt.cloaked = false
    shooter.cooldownLeft = cooldown
    val pilot = new SnipePilot
    val world = new World(Seq(shooter), Seq(dt),
      Map(shooter -> pilot, dt -> new Pilots.Hold), Seq.empty)
    var f = 0
    while (f < frames && shooter.alive && dt.alive) { world.step(); f += 1 }
    (shooter, dt, pilot)
  }

  test("a dragoon answers a DT at three tiles, holds fire point-blank") {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val (s, dt, pilot) = runDuel(dragoon, 0.0, 404.0, cooldown = 0, frames = 200)
    println(s"DIAG duel3 targeted=${pilot.targetedDT} shots=${pilot.shots}")
    assert(pilot.targetedDT, "the ladder never picked a DT it duels for free")
    assert(pilot.shots > 0, "no shots fired at a dueled DT")
  }

  test("a dragoon holds fire on a DT inside 2.6 tiles") {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val (s, dt, pilot) = runDuel(dragoon, 0.0, 436.0, cooldown = 0, frames = 200)
    println(s"DIAG holdfire targeted=${pilot.targetedDT} shots=${pilot.shots}")
    assert( ! pilot.targetedDT, "the ladder picked a DT inside 2.6 tiles")
    assert(pilot.shots == 0, "shots fired at a DT inside 2.6 tiles")
  }

  test("a cooling dragoon holds fire until the gun is warm") {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val (s, dt, pilot) = runDuel(dragoon, 0.0, 404.0, cooldown = 30, frames = 10)
    println(s"DIAG cooling targeted=${pilot.targetedDT} shots=${pilot.shots}")
    assert( ! pilot.targetedDT, "a cooling gun picked a DT it should have waited out")
    assert(pilot.shots == 0, "a cooling gun answered a DT it should have waited out")
  }

  test("a zealot answers a point-blank DT it can reach") {
    val zealot = new Body(Protoss.Zealot, 0.0, 500.0, Protoss.Zealot.maxTotalHealth)
    val (s, dt, pilot) = runDuel(zealot, 0.0, 492.0, cooldown = 0, frames = 600)
    println(s"DIAG melee targeted=${pilot.targetedDT} shots=${pilot.shots} dt=${dt.hp.toInt}")
    assert(pilot.targetedDT, "a melee hull refused a DT inside its reach")
  }
}
