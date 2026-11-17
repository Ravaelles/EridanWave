package starengine

import Mathematics.Points.Pixel
import Micro.Actions.Combat.Maneuvering.ArtilleryKeepOff
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Siege keep-off, step by step: a Dragoon retreats along a waypoint line
 * that crosses a sieged tank's fire zone. The plain retreat walks the
 * line into gun reach and dies there (the field report: Retreat-mission
 * Dragoons entering siege fire). The keep-off pilot calls the shipped
 * ArtilleryKeepOff gate - inside 12.8 plus wound tiles, without a battle
 * verdict, it steps one tile past the line; outside it walks the
 * retreat. The survivor never enters shell reach although the waypoint
 * pulls it back in every frame; a hull with a battle verdict ignores
 * the line and walks in. Speeds, reaches and damage are the real
 * UnitType numbers; the decision is the shipped kernel. Visibility has
 * no engine model - every body reads as detected, the production gate
 * for cloaked hulls is covered by the base suite contract.
 */
class ArtilleryKeepOffScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Artillery.name + StarEngineTags.Dragoon.name) }

  private val goal = (1600.0, 500.0)

  /** Plain retreat: the waypoint line is the whole plan. */
  private class RetreatPilot extends Pilot {

    override def direct(me: Body, world: World): Directive = MoveTo(goal._1, goal._2)
  }

  /**
   * Keep-off pilot, production order: the shipped keep-off against the
   * wound while no battle is wanted, the shipped step past the line,
   * the retreat waypoint otherwise.
   */
  private class KeepOffPilot(wantBattle: Boolean) extends Pilot {
    var yields = 0

    override def direct(me: Body, world: World): Directive = {
      val tank = world.foes.filter(f => f.alive && f.kind == Terran.SiegeTankSieged).sortBy(me.dist).headOption
      tank match {
        case Some(t) if ! wantBattle && ArtilleryKeepOff.shouldYield(me.dist(t) / 32.0, me.lossFraction) =>
          yields += 1
          val dest = ArtilleryKeepOff.fallback(
            Pixel(me.x.toInt, me.y.toInt),
            Pixel(t.x.toInt, t.y.toInt),
            ArtilleryKeepOff.keepOffTiles(me.lossFraction))

          MoveTo(dest.x, dest.y)
        case _ => MoveTo(goal._1, goal._2)
      }
    }
  }

  private def runDragoon(pilot: Pilot, startHpFraction: Double): (Body, Body, Int, Double) = {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 500.0, Protoss.Dragoon.maxTotalHealth * startHpFraction)
    val tank = new Body(Terran.SiegeTankSieged, 800.0, 500.0, Terran.SiegeTankSieged.maxTotalHealth)
    val world = new World(Seq(dragoon), Seq(tank), Map(dragoon -> pilot, tank -> new Pilots.Hold), Seq.empty)

    var minDist = Double.PositiveInfinity
    while (world.frame < 3000 && dragoon.alive) {
      world.step()
      if (tank.alive) minDist = Math.min(minDist, dragoon.dist(tank))
    }

    (dragoon, tank, world.frame, minDist)
  }

  test("a plain retreat walks the waypoint line into shell reach and dies") {
    val (dragoon, tank, frames, minDist) = runDragoon(new RetreatPilot, startHpFraction = 1.0)
    val reach = tank.rangeAgainst(dragoon)
    println(s"DIAG retreat reach=$reach minDist=${minDist.toInt}px frames=$frames alive=${dragoon.alive}")
    assert( ! dragoon.alive, "retreat survived the siege line - no problem to fix")
  }

  test("a keep-off retreat never enters shell reach and lives") {
    val pilot = new KeepOffPilot(wantBattle = false)
    val (dragoon, tank, frames, minDist) = runDragoon(pilot, startHpFraction = 0.5)
    val reach = tank.rangeAgainst(dragoon)
    println(s"DIAG keepoff reach=$reach minDist=${minDist.toInt}px frames=$frames yields=${pilot.yields} alive=${dragoon.alive}")
    assert(pilot.yields > 0, "keep-off never fired on a line through a sieged tank")
    assert(dragoon.alive, "keep-off retreat died outside the line")
    assert(minDist > reach, f"dragoon dipped inside shell reach: $minDist%.0fpx <= $reach%.0fpx")
  }

  test("a battle verdict ignores the line and walks in") {
    val pilot = new KeepOffPilot(wantBattle = true)
    val (dragoon, tank, frames, minDist) = runDragoon(pilot, startHpFraction = 1.0)
    val reach = tank.rangeAgainst(dragoon)
    println(s"DIAG battle reach=$reach minDist=${minDist.toInt}px frames=$frames yields=${pilot.yields}")
    assert(pilot.yields == 0, "keep-off stole a hull that wants the fight")
    assert(minDist <= reach, "battle hull never closed the tank it wants")
  }
}
