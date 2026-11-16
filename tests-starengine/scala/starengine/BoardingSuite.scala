package starengine

import Micro.Actions.Protoss.ReaverShuttle.ReaverBoardGate
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Boarding, step by step: a Reaver clears Marines with its Shuttle at
 * its side. The gate pilot calls the shipped ReaverBoardGate - quiet
 * (no shot in ten seconds) and crowded (a foe inside twenty tiles) pins
 * the hull on the ground, a recent shot opens the ride. First the lock:
 * the hull fights on foot next to an adjacent ride and never embarks.
 * Then the release: after the kill the same hull boards. A hull with
 * nowhere to go unloads where the ride floats. Speeds, reaches, cargo
 * slots and damage are the real UnitType numbers; the lock decision is
 * the shipped kernel, the embark is the engine's Board/Unload.
 */
class BoardingSuite extends AnyFunSuite with StarEngineSuite {

  /**
   * Gate pilot, production order: the shipped lock on the pilot's own
   * shot clock and the nearest foe; while open the hull wants the ride
   * (boards inside reach, walks back otherwise, like boardRide);
   * while locked it works foes on foot inside ten tiles and otherwise
   * holds by the ride instead of roaming off it.
   */
  private class GatePilot extends Pilot {
    var lastAttackFrame: Int = -100000
    var boards = 0

    override def direct(me: Body, world: World): Directive = {
      val ride = world.ours.find(o => o.alive && o.kind == Protoss.Shuttle)
      val foes = world.foes.filter(_.alive)
      val nearestTiles = foes.sortBy(me.dist).headOption.map(me.dist(_) / 32.0).getOrElse(Double.PositiveInfinity)
      val locked = ReaverBoardGate.locked(world.frame - lastAttackFrame, nearestTiles)
      if ( ! locked) {
        ride match {
          case Some(r) if me.dist(r) <= World.BoardRangePx =>
            boards += 1

            Board(r)
          case Some(r) => MoveTo(r.x, r.y)
          case None    => Hold
        }
      } else {
        foes.sortBy(me.dist).headOption match {
          case Some(f) if me.dist(f) <= 10.0 * 32.0 =>
            // Decision-phase sees pre-move positions, the guns resolve
            // post-move: one stride of margin aligns the clock with the
            // shot instead of trailing it by a frame.
            if (me.dist(f) <= me.rangeAgainst(f) + me.speed) lastAttackFrame = world.frame

            Chase(f)
          case _ => Hold
        }
      }
    }
  }

  private def runGate(stop: (Body, Body) => Boolean): (Body, Body, Body, GatePilot, Int) = {
    val reaver = new Body(Protoss.Reaver, 0.0, 500.0, Protoss.Reaver.maxTotalHealth)
    val marine1 = new Body(Terran.Marine, 300.0, 500.0, Terran.Marine.maxTotalHealth)
    val marine2 = new Body(Terran.Marine, 480.0, 500.0, Terran.Marine.maxTotalHealth)
    val shuttle = new Body(Protoss.Shuttle, 32.0, 500.0, Protoss.Shuttle.maxTotalHealth)
    val pilot = new GatePilot
    val pilots: Map[Body, Pilot] = Map(
      reaver -> pilot,
      marine1 -> new Pilots.Hold,
      marine2 -> new Pilots.Hold,
      shuttle -> new Pilots.Hold)
    val world = new World(Seq(reaver, shuttle), Seq(marine1, marine2), pilots, Seq.empty)

    while (world.frame < 3000 && reaver.alive && ! stop(marine1, reaver)) {
      world.step()
    }

    (reaver, marine1, marine2, pilot, world.frame)
  }

  test("a quiet crowded hull fights on foot and never embarks") {
    val (reaver, marine1, marine2, pilot, frames) = runGate((m1, _) => ! m1.alive)
    println(s"DIAG locked frames=$frames boards=${pilot.boards} reaver=${reaver.hp.toInt} marine1=${marine1.hp.toInt} loaded=${reaver.transport.isDefined}")
    assert( ! marine1.alive, "locked hull never cleared its side")
    assert(pilot.boards == 0, "quiet crowded hull embarked next to an adjacent ride")
    assert(reaver.transport.isEmpty, "quiet crowded hull is loaded")
    assert(reaver.alive, "locked hull died fighting on foot")
    assert(marine2.alive, "locked hull chased past its side")
  }

  test("after the shot the same hull boards the adjacent ride") {
    val (reaver, marine1, marine2, pilot, frames) = runGate((_, r) => r.transport.isDefined)
    println(s"DIAG released frames=$frames boards=${pilot.boards} loaded=${reaver.transport.isDefined} reaver=${reaver.hp.toInt}")
    assert(reaver.transport.isDefined, "recently-fired hull never boarded the adjacent ride")
    assert(pilot.boards > 0, "no embark order was ever issued")
    assert(marine2.alive, "released hull kept fighting instead of embarking")
  }

  test("a hull with nowhere to go unloads where the ride floats") {
    val reaver = new Body(Protoss.Reaver, 0.0, 500.0, Protoss.Reaver.maxTotalHealth)
    val shuttle = new Body(Protoss.Shuttle, 32.0, 500.0, Protoss.Shuttle.maxTotalHealth)
    val pilot = new Pilot {
      override def direct(me: Body, world: World): Directive =
        if (world.frame < 5) Board(shuttle) else Unload
    }
    val world = new World(Seq(reaver, shuttle), Seq.empty,
      Map(reaver -> pilot, shuttle -> new Pilots.Hold), Seq.empty)

    while (world.frame < 5 && reaver.transport.isEmpty) {
      world.step()
    }
    assert(reaver.transport.isDefined, "adjacent hull never embarked")
    val (hx, hy) = (shuttle.x, shuttle.y)
    while (world.frame < 20 && reaver.transport.isDefined) {
      world.step()
    }
    println(s"DIAG unload loaded=${reaver.transport.isDefined} at=(${reaver.x.toInt},${reaver.y.toInt}) hull=(${hx.toInt},${hy.toInt})")
    assert(reaver.transport.isEmpty, "hull never disembarked")
    assert(reaver.x == hx && reaver.y == hy, "hull did not land where the ride floats")
  }
}
