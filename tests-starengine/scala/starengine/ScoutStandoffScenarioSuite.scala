package starengine

import Micro.Actions.Combat.Decisioning.ScoutStaticKeepOff
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Scout static keep-off, step by step: a Scout duels a Carrier parked
 * under a Missile Turret. The pilot calls the shipped holdPoint geometry
 * on the same facts production reads (static reach, target and weapon
 * range) and stands outside reach plus margin while the gun reaches -
 * instead of posing inside the guns like the naked approach does. The
 * hold point moves only the feet; every shot in the scenario is the
 * engine's own. Speeds, reaches and damage are the real UnitType numbers.
 */
class ScoutStandoffScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Scout.name + StarEngineTags.Static.name) }

  /** Pilot, production order: the shipped geometry holds, legs walk. */
  private class StandoffPilot(static: Body, target: Body) extends Pilot {
    var holds = 0
    var minStaticDist = Double.PositiveInfinity

    override def direct(me: Body, world: World): Directive = {
      val hold = ScoutStaticKeepOff.holdPoint(
        staticX = static.x, staticY = static.y,
        staticTileWidth = static.kind.tileWidth, staticTileHeight = static.kind.tileHeight,
        staticRangePixels = static.rangeAgainst(me),
        targetX = target.x, targetY = target.y,
        weaponRangePixels = me.rangeAgainst(target))
      minStaticDist = Math.min(minStaticDist, me.dist(static))
      hold match {
        case Some((hx, hy)) if me.dist(static) <= static.rangeAgainst(me) + ScoutStaticKeepOff.marginTiles * 32.0 =>
          holds += 1
          MoveTo(hx, hy)
        case _ =>
          target match {
            case t if t.alive => Chase(t)
            case _            => Hold
          }
      }
    }
  }

  test("the duel holds outside static reach instead of posing inside it") {
    val scout = new Body(Protoss.Scout, 200.0, 500.0, Protoss.Scout.maxTotalHealth)
    val carrier = new Body(Protoss.Carrier, 400.0, 500.0, Protoss.Carrier.maxTotalHealth)
    val turret = new Body(Terran.MissileTurret, 650.0, 500.0, Terran.MissileTurret.maxTotalHealth)
    val pilot = new StandoffPilot(turret, carrier)
    val world = new World(Seq(scout), Seq(carrier, turret),
      Map(scout -> pilot, carrier -> new Pilots.Hold, turret -> new Pilots.Hold), Seq.empty)

    var f = 0
    while (f < 1200 && scout.alive && carrier.alive) {
      world.step()
      f = world.frame
    }
    val keepOut = turret.rangeAgainst(scout) + ScoutStaticKeepOff.marginTiles * 32.0
    println(s"DIAG standoff holds=${pilot.holds} minStatic=${pilot.minStaticDist} keepOut=$keepOut scout=${scout.hp} carrier=${carrier.hp}")
    assert(pilot.holds > 0, "standoff never engaged")
    assert(pilot.minStaticDist >= keepOut - 1.0, "duelist entered static reach")
    assert(carrier.hp < Protoss.Carrier.maxTotalHealth, "carrier never took fire from the hold")
  }

  test("a target under the umbrella keeps the normal duel") {
    val scout = new Body(Protoss.Scout, 0.0, 500.0, Protoss.Scout.maxTotalHealth)
    val carrier = new Body(Protoss.Carrier, 150.0, 500.0, Protoss.Carrier.maxTotalHealth)
    val turret = new Body(Terran.MissileTurret, 200.0, 500.0, Terran.MissileTurret.maxTotalHealth)
    val pilot = new StandoffPilot(turret, carrier)
    val world = new World(Seq(scout), Seq(carrier, turret),
      Map(scout -> pilot, carrier -> new Pilots.Hold, turret -> new Pilots.Hold), Seq.empty)

    var f = 0
    while (f < 1200 && scout.alive && carrier.alive) {
      world.step()
      f = world.frame
    }
    println(s"DIAG umbrella holds=${pilot.holds} carrier=${carrier.hp}")
    assert(pilot.holds == 0, "hold engaged where no hold reaches the victim")
  }
}
