package starengine

import Mathematics.Points.Pixel
import Micro.Actions.Combat.Maneuvering.AirStaticStandoff
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Static-AA standoff, step by step: an Observer crosses a Missile Turret
 * lane on a mission goal. The crosser flies the straight line and dies
 * inside the gun's reach (the field report); the standoff pilot calls
 * the shipped AirStaticStandoff gate - inside eight plus wound tiles it
 * backs six tiles out with jitter on top, outside it walks the mission.
 * The survivor never enters gun reach despite the mission pulling it
 * back in every frame. Speeds, reaches and damage are the real UnitType
 * numbers; the decision is the shipped kernel.
 */
class AirStandoffScenarioSuite extends AnyFunSuite with StarEngineSuite {

  private val goal = (2000.0, 500.0)

  /** Straight line, no doctrine: the field behavior. */
  private class CrossPilot extends Pilot {

    override def direct(me: Body, world: World): Directive = MoveTo(goal._1, goal._2)
  }

  /**
   * Standoff pilot, production order: nearest static AA gun, the shipped
   * keep-off against the wound, the shipped six-tile fallback with a
   * fixed jitter (suites pin pixels, the game rolls +-2 tiles).
   */
  private class StandPilot(jitterTiles: (Double, Double)) extends Pilot {
    var yields = 0

    override def direct(me: Body, world: World): Directive = {
      val gun = world.foes.filter(f => f.alive && f.kind.isBuilding && f.kind.attacksAir).sortBy(me.dist).headOption
      gun match {
        case Some(g) if AirStaticStandoff.shouldYield(me.dist(g) / 32.0, me.lossFraction) =>
          yields += 1
          val dest = AirStaticStandoff.fallback(
            Pixel(me.x.toInt, me.y.toInt),
            Pixel(g.x.toInt, g.y.toInt),
            jitterTiles._1 * 32.0,
            jitterTiles._2 * 32.0)

          MoveTo(dest.x, dest.y)
        case _ => MoveTo(goal._1, goal._2)
      }
    }
  }

  private def runEye(pilot: Pilot): (Body, Body, Int, Double) = {
    val eye = new Body(Protoss.Observer, 0.0, 500.0, Protoss.Observer.maxTotalHealth)
    val turret = new Body(Terran.MissileTurret, 1000.0, 500.0, Terran.MissileTurret.maxTotalHealth)
    val world = new World(Seq(eye), Seq(turret), Map(eye -> pilot, turret -> new Pilots.Hold), Seq.empty)

    var minDist = Double.PositiveInfinity
    while (world.frame < 3000 && eye.alive) {
      world.step()
      if (turret.alive) minDist = Math.min(minDist, eye.dist(turret))
    }

    (eye, turret, world.frame, minDist)
  }

  test("a straight crossing flies into the gun and dies") {
    val (eye, turret, frames, minDist) = runEye(new CrossPilot)
    val reach = turret.rangeAgainst(eye)
    println(s"DIAG cross reach=$reach minDist=${minDist.toInt}px frames=$frames alive=${eye.alive}")
    assert( ! eye.alive, "crosser survived the turret lane - no problem to fix")
  }

  test("a standoff eye never enters gun reach and lives") {
    val pilot = new StandPilot((1.0, -1.0))
    val (eye, turret, frames, minDist) = runEye(pilot)
    val reach = turret.rangeAgainst(eye)
    println(s"DIAG stand reach=$reach minDist=${minDist.toInt}px frames=$frames yields=${pilot.yields} alive=${eye.alive}")
    assert(pilot.yields > 0, "standoff never fired on a lane through a gun")
    assert(eye.alive, "standoff eye died outside the keep-off")
    assert(minDist > reach, f"eye dipped inside gun reach: $minDist%.0fpx <= $reach%.0fpx")
  }
}
