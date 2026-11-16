package starengine

import Micro.Actions.Combat.Decisioning.DragoonTerranStand
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Dragoon stand vs Terran, step by step: a Dragoon meets a pressing
 * Marine. The retreat verdict issues no attack order - the suite reads
 * that as a passive hull holding still - and the Marine walks it down
 * for free (the field report: Dragoons dying instead of shooting).
 * The stand pilot calls the shipped DragoonTerranStand gate: fresh
 * shields and a hot gun with a Terran in range commits to the kill,
 * otherwise the hull keeps no orders. A chewed hull and a cold gun
 * stay order-less end to end, which is exactly the gate. Speeds,
 * reaches and damage are the real UnitType numbers; the decision is
 * the shipped kernel.
 */
class DragoonStandScenarioSuite extends AnyFunSuite with StarEngineSuite {

  /** No attack order: the retreat verdict's output, a passive hull. */
  private class NoOrdersPilot extends Pilot {

    override def direct(me: Body, world: World): Directive = Hold
  }

  /**
   * Stand pilot, production order: the shipped stand gate on the pooled
   * bar (fresh hulls read fresh shields) and the live cooldown, the
   * nearest Terran in range as the prey, no orders otherwise.
   */
  private class StandPilot extends Pilot {
    var stands = 0

    override def direct(me: Body, world: World): Directive = {
      val prey = world.foes
        .filter(f => f.alive && f.kind.isTerran && me.canHit(f) && me.dist(f) <= me.rangeAgainst(f))
        .sortBy(me.dist).headOption
      val shieldWound = Math.min(80, (me.maxHp - me.hp).toInt)
      if (prey.exists(p => DragoonTerranStand.shouldStand(shieldWound, me.cooldownLeft, terranInRange = true))) {
        stands += 1

        Chase(prey.get)
      } else Hold
    }
  }

  private def runDuel(pilot: Pilot, startHp: Double, startCooldown: Int, passive: Boolean): (Body, Body, Int) = {
    val dragoon = new Body(Protoss.Dragoon, 400.0, 500.0, startHp)
    dragoon.cooldownLeft = startCooldown
    // Passive reads as no attack order: the engine only shoots on orders
    // or contact, so a hull the rule stays silent on must hold its fire
    // or the contact autoshoot proves nothing.
    dragoon.passive = passive
    val marine = new Body(Terran.Marine, 700.0, 500.0, Terran.Marine.maxTotalHealth)
    val pilots: Map[Body, Pilot] = Map(
      dragoon -> pilot,
      marine -> new Pilots.ZombieAttack(_.ours, (400.0, 500.0)))
    val world = new World(Seq(dragoon), Seq(marine), pilots, Seq.empty)

    while (world.frame < 3000 && dragoon.alive && marine.alive) {
      world.step()
    }

    (dragoon, marine, world.frame)
  }

  test("a dragoon with no orders dies to a pressing marine for free") {
    val (dragoon, marine, frames) = runDuel(new NoOrdersPilot, Protoss.Dragoon.maxTotalHealth, 0, passive = true)
    println(s"DIAG noorders frames=$frames dragoon=${dragoon.hp.toInt} marine=${marine.hp.toInt}")
    assert( ! dragoon.alive, "order-less dragoon survived - no problem to fix")
    assert(marine.hp == marine.maxHp, "order-less dragoon dealt damage somehow")
  }

  test("a fresh hot dragoon stands and kills the marine") {
    val pilot = new StandPilot
    val (dragoon, marine, frames) = runDuel(pilot, Protoss.Dragoon.maxTotalHealth, 0, passive = false)
    println(s"DIAG stand frames=$frames stands=${pilot.stands} dragoon=${dragoon.hp.toInt} marine=${marine.hp.toInt}")
    assert(pilot.stands > 0, "stand never fired on a fresh hot dragoon")
    assert( ! marine.alive, "standing dragoon never killed the marine")
    assert(dragoon.alive, "dragoon died winning a duel it outguns twenty to one")
  }

  test("a chewed dragoon keeps no orders and dies") {
    val pilot = new StandPilot
    val (dragoon, marine, frames) =
      runDuel(pilot, Protoss.Dragoon.maxTotalHealth - DragoonTerranStand.maxShieldWound - 1, 0, passive = true)
    println(s"DIAG chewed frames=$frames stands=${pilot.stands} dragoon=${dragoon.hp.toInt} marine=${marine.hp.toInt}")
    assert(pilot.stands == 0, "chewed hull stood anyway")
    assert( ! dragoon.alive, "chewed dragoon survived without orders")
  }

  test("a cold gun keeps no orders until it goes hot") {
    val pilot = new StandPilot
    val dragoon = new Body(Protoss.Dragoon, 400.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    dragoon.cooldownLeft = DragoonTerranStand.readyCooldownFrames + 24
    val marine = new Body(Terran.Marine, 450.0, 500.0, Terran.Marine.maxTotalHealth)
    val world = new World(Seq(dragoon), Seq(marine),
      Map(dragoon -> pilot, marine -> new Pilots.ZombieAttack(_.ours, (400.0, 500.0))), Seq.empty)

    // In range from frame zero, so only the cooldown can be holding the stand back.
    while (world.frame < DragoonTerranStand.readyCooldownFrames && dragoon.alive && marine.alive) {
      world.step()
    }

    println(s"DIAG cold frames=${world.frame} stands=${pilot.stands} dragoon=${dragoon.hp.toInt}")
    assert(pilot.stands == 0, "cold gun stood before going hot")

    while (world.frame < 40 && dragoon.alive && marine.alive) {
      world.step()
    }

    assert(pilot.stands > 0, "gun never stood after going hot")
  }
}
