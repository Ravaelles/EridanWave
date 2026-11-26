package starengine

import ProxyBwapi.Races.Protoss
import Tactic.Squads.Decisions.{ClusterPosture, RetreatExtraPenalties}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Posture hold, step by step: a Dragoon line meets a superior mirror and
 * the shared verdict breaks off. The pilot calls the shipped
 * ClusterPosture kernel on the same facts production reads (sim
 * agreement, penalized advantage, contact) and obeys it: Withdraw means
 * walking home, anything else means closing. The test pins the two
 * temporal guarantees no single-frame row can: a Withdrawal holds its
 * floor through flickering forecasts, and the Fight edge carries its
 * deadband instead of flapping on the line.
 */
class PostureHoldSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Posture.name + StarEngineTags.Retreat.name) }

  /** Pilot, production order: the shipped kernel decides, legs execute. */
  private class PosturePilot extends Pilot {
    var posture: ClusterPosture.Posture = ClusterPosture.Fight
    var withdrawStart: Int = 0
    var withdrawals: Int = 0

    var fighting: Double = 1.0
    var advantage: Double = 2.0

    override def direct(me: Body, world: World): Directive = {
      val raw = advantage
      val penalized = raw * RetreatExtraPenalties.advantageMultiplier(world.frame - withdrawStart)
      val next = ClusterPosture.next(posture, fighting, 1.0, penalized, inContact = true, world.frame - withdrawStart)
      if (next == ClusterPosture.Withdraw && posture != ClusterPosture.Withdraw) withdrawStart = world.frame
      if (next == ClusterPosture.Withdraw) withdrawals += 1
      posture = next
      if (posture == ClusterPosture.Withdraw) {
        val foe = world.foes.find(_.alive).get
        val dx = me.x - foe.x
        val dy = me.y - foe.y
        val dist = Math.sqrt(dx * dx + dy * dy)
        if (dist > 0) MoveTo(me.x + dx / dist * 200.0, me.y + dy / dist * 200.0)
        else Hold
      } else {
        world.foes.find(_.alive).map(Chase).getOrElse(Hold)
      }
    }
  }

  test("a withdrawal holds its floor through flickering forecasts") {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val foe = new Body(Protoss.Dragoon, 400.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val pilot = new PosturePilot
    val world = new World(Seq(dragoon), Seq(foe),
      Map(dragoon -> pilot, foe -> new Pilots.Hold), Seq.empty)

    var enteredAt = -1
    var leftAt = -1
    var f = 0
    while (f < 400 && dragoon.alive) {
      // Collapse at frame 10; the advantage flickers above parity after.
      pilot.fighting = if (world.frame < 10) 1.0 else 0.2
      pilot.advantage = if (world.frame < 10) 2.0
      else if (world.frame % 20 < 10) 0.8
      else 1.3
      world.step()
      f = world.frame
      if (enteredAt < 0 && pilot.posture == ClusterPosture.Withdraw) enteredAt = f
      if (enteredAt >= 0 && leftAt < 0 && pilot.posture != ClusterPosture.Withdraw) leftAt = f
    }
    println(s"DIAG hold entered=$enteredAt left=$leftAt withdrawals=${pilot.withdrawals}")
    assert(enteredAt >= 0, "sim collapse never broke the line")
    assert(leftAt == -1 || leftAt - enteredAt >= 96, "withdrawal left before its floor")
    assert(pilot.withdrawals > 96, "line did not hold the retreat")
  }

  test("the floor releases into a re-engage once the forecast clears") {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val foe = new Body(Protoss.Dragoon, 400.0, 500.0, Protoss.Dragoon.maxTotalHealth)
    val pilot = new PosturePilot
    val world = new World(Seq(dragoon), Seq(foe),
      Map(dragoon -> pilot, foe -> new Pilots.Hold), Seq.empty)

    var f = 0
    while (f < 500 && dragoon.alive && foe.alive) {
      if (world.frame < 10) { pilot.fighting = 1.0; pilot.advantage = 2.0 }
      else if (world.frame < 200) { pilot.fighting = 0.2; pilot.advantage = 0.8 }
      else { pilot.fighting = 0.9; pilot.advantage = 2.0 }
      world.step()
      f = world.frame
    }
    println(s"DIAG release posture=${pilot.posture} frame=$f")
    assert(pilot.posture == ClusterPosture.Fight, "clear forecast never re-engaged, got " + pilot.posture)
  }
}
