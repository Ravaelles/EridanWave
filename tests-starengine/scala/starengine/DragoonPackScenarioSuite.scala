package starengine

import Micro.Actions.Protoss.Dragoon.DragoonPackStand
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Dragoon pack stand, step by step: three Dragoons meet two pressing
 * Vultures. The retreat verdict runs for the map edge while the pack
 * shoots over its shoulder; Vultures are faster, so the run ends caught
 * and dead (the field report: packs retreating from light Terran). The
 * stand pilot calls the shipped DragoonPackStand gate - more than two
 * Dragoons, nothing but light Terran around, hulls above 42 - and holds
 * the line instead. A chewed pack, a pair and a pack with shells in
 * contact keep no stand, which is exactly each gate. Speeds, reaches
 * and damage are the real UnitType numbers; the decision is the shipped
 * kernel.
 */
class DragoonPackScenarioSuite extends AnyFunSuite with StarEngineSuite {

  private val goal = (-800.0, 500.0)

  /** Retreat verdict: run for the edge, shooting over the shoulder. */
  private class FleePilot extends Pilot {

    override def direct(me: Body, world: World): Directive = MoveTo(goal._1, goal._2)
  }

  /**
   * Pack pilot, production order: the shipped stand gate on the live
   * pack, the live threats and the pooled hull, the nearest foe as the
   * prey, the edge otherwise.
   */
  private class PackPilot extends Pilot {
    var stands = 0

    override def direct(me: Body, world: World): Directive = {
      val pack = world.ours.count(o => o.alive && o.kind == Protoss.Dragoon)
      val foes = world.foes.filter(_.alive)
      val lightOnly = foes.nonEmpty && foes.forall(f =>
        f.kind == Terran.Vulture || f.kind == Terran.Marine || f.kind == Terran.Medic || f.kind == Terran.Firebat)
      if (DragoonPackStand.shouldStand(pack, lightOnly, me.hp.toInt)) {
        stands += 1

        world.nearestFoeOf(me, world.foes, (m, f) => m.canHit(f)) match {
          case Some(foe) => Chase(foe)
          case None      => Hold
        }
      } else MoveTo(goal._1, goal._2)
    }
  }

  private def runPack(
    pilot: Pilot,
    dragoonHp: Double,
    dragoonCount: Int,
    foes: Seq[Body],
    foePilot: Body => Pilot): (Seq[Body], Seq[Body], Int, Int) = {
    val dragoons = (0 until dragoonCount).map(i =>
      new Body(Protoss.Dragoon, -60.0 * i, 500.0, dragoonHp)).toSeq
    val pilots: Map[Body, Pilot] =
      dragoons.map(_ -> pilot).toMap ++ foes.map(f => f -> foePilot(f)).toMap
    val world = new World(dragoons, foes, pilots, Seq.empty)

    while (world.frame < 4000 && dragoons.exists(_.alive) && foes.exists(_.alive)) {
      world.step()
    }

    (dragoons, foes, world.frame, pilot.asInstanceOf[PackPilot].stands)
  }

  private def vultures(n: Int): Seq[Body] =
    (0 until n).map(i => new Body(Terran.Vulture, 600.0 + 60.0 * i, 500.0, Terran.Vulture.maxTotalHealth)).toSeq

  test("a retreating pack abandons the line it could hold") {
    val foePilots = new scala.collection.mutable.HashMap[Body, Pilot]()
    val (dragoons, foes, frames, _) = runPack(new PackPilot {
      override def direct(me: Body, world: World): Directive = MoveTo(goal._1, goal._2)
    }, Protoss.Dragoon.maxTotalHealth, 3, vultures(2), f => new Pilots.ZombieAttack(_.ours, (0.0, 500.0)))
    val line = dragoons.map(_.x).max
    println(s"DIAG flee frames=$frames line=$line dragoons=${dragoons.count(_.alive)}/3 vultures=${foes.count(_.alive)}/2")
    assert(line < -400, f"retreat held the line at $line%.0f - no problem to fix")
  }

  test("a pack stands, holds the line and kills the vultures") {
    val pilot = new PackPilot
    val (dragoons, foes, frames, stands) =
      runPack(pilot, Protoss.Dragoon.maxTotalHealth, 3, vultures(2), _ => new Pilots.ZombieAttack(_.ours, (0.0, 500.0)))
    val line = dragoons.map(_.x).max
    println(s"DIAG stand frames=$frames stands=$stands line=$line dragoons=${dragoons.count(_.alive)}/3 vultures=${foes.count(_.alive)}/2")
    assert(stands > 0, "stand never fired on three fresh dragoons vs vultures")
    assert(foes.forall(! _.alive), "standing pack never killed the vultures")
    assert(dragoons.exists(_.alive), "pack died winning a duel it outguns")
    assert(line > -200, f"stand abandoned the line at $line%.0f")
  }

  test("a chewed pack keeps no stand") {
    val pilot = new PackPilot
    val (dragoons, foes, frames, stands) =
      runPack(pilot, DragoonPackStand.retreatHp.toDouble, 3, vultures(2), _ => new Pilots.ZombieAttack(_.ours, (0.0, 500.0)))
    println(s"DIAG chewed frames=$frames stands=$stands dragoons=${dragoons.count(_.alive)}/3")
    assert(stands == 0, "chewed pack stood anyway")
  }

  test("a pair keeps no stand") {
    val pilot = new PackPilot
    val (dragoons, foes, frames, stands) =
      runPack(pilot, Protoss.Dragoon.maxTotalHealth, 2, vultures(2), _ => new Pilots.ZombieAttack(_.ours, (0.0, 500.0)))
    println(s"DIAG pair frames=$frames stands=$stands dragoons=${dragoons.count(_.alive)}/2")
    assert(stands == 0, "a pair stood anyway")
  }

  test("shells in contact break the stand") {
    val pilot = new PackPilot
    val tank = new Body(Terran.SiegeTankSieged, 700.0, 500.0, Terran.SiegeTankSieged.maxTotalHealth)
    val foes = vultures(2) :+ tank
    val (dragoons, _, frames, stands) =
      runPack(pilot, Protoss.Dragoon.maxTotalHealth, 3, foes, {
        case b if b.kind == Terran.SiegeTankSieged => new Pilots.Hold
        case _ => new Pilots.ZombieAttack(_.ours, (0.0, 500.0))
      })
    println(s"DIAG heavy frames=$frames stands=$stands dragoons=${dragoons.count(_.alive)}/3 tank=${tank.hp.toInt}")
    assert(stands == 0, "pack stood with shells in contact")
  }
}
