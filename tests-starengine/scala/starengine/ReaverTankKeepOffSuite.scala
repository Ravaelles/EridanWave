package starengine

import Mathematics.Points.Pixel
import Micro.Actions.Combat.Maneuvering.ReaverTankKeepOff
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Reaver tank keep-off, step by step: a half-hull Reaver meets two
 * Marines with a sieged tank between them. The plain hunter chews the
 * nearest foe, then walks at the tank and dies under the shells (the
 * field report: Reavers approaching guns). The keep-off pilot calls the
 * shipped ReaverTankKeepOff gate - inside 12.5 tiles of the tank it steps
 * one tile past the line, otherwise it hunts only the other units. The
 * survivor clears its side, holds the line and never enters shell reach.
 * Speeds, reaches and damage are the real UnitType numbers; the decision
 * is the shipped kernel.
 */
class ReaverTankKeepOffSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Reaver.name + StarEngineTags.Artillery.name) }

  /** Plain hunter: nearest foe, tanks included. */
  private class HuntAllPilot extends Pilot {

    override def direct(me: Body, world: World): Directive =
      world.nearestFoeOf(me, world.foes, (m, f) => m.canHit(f)) match {
        case Some(foe) => Chase(foe)
        case None      => Hold
      }
  }

  /**
   * Keep-off hunter, production order: the shipped line against the
   * nearest sieged tank first, the nearest non-tank prey otherwise -
   * the hull only hunts other units.
   */
  private class HuntOthersPilot extends Pilot {
    var yields = 0

    override def direct(me: Body, world: World): Directive = {
      val tank = world.foes.filter(f => f.alive && f.kind == Terran.SiegeTankSieged).sortBy(me.dist).headOption
      if (tank.exists(t => ReaverTankKeepOff.shouldYield(me.dist(t) / 32.0))) {
        yields += 1
        val dest = ReaverTankKeepOff.fallback(
          Pixel(me.x.toInt, me.y.toInt),
          Pixel(tank.get.x.toInt, tank.get.y.toInt))

        MoveTo(dest.x, dest.y)
      } else {
        world.foes.filter(f => f.alive && f.kind != Terran.SiegeTankSieged && me.canHit(f)).sortBy(me.dist).headOption match {
          case Some(prey) => Chase(prey)
          case None       => Hold
        }
      }
    }
  }

  private def runReaver(pilot: Pilot): (Body, Body, Body, Body, Int, Double) = {
    val reaver = new Body(Protoss.Reaver, 0.0, 500.0, Protoss.Reaver.maxTotalHealth / 2)
    val marine1 = new Body(Terran.Marine, 300.0, 500.0, Terran.Marine.maxTotalHealth)
    val tank = new Body(Terran.SiegeTankSieged, 800.0, 500.0, Terran.SiegeTankSieged.maxTotalHealth)
    val marine2 = new Body(Terran.Marine, 1100.0, 500.0, Terran.Marine.maxTotalHealth)
    val pilots: Map[Body, Pilot] = Map(
      reaver -> pilot,
      marine1 -> new Pilots.Hold,
      tank -> new Pilots.Hold,
      marine2 -> new Pilots.Hold)
    val world = new World(Seq(reaver), Seq(marine1, tank, marine2), pilots, Seq.empty)

    var minDist = Double.PositiveInfinity
    while (world.frame < 4000 && reaver.alive) {
      world.step()
      if (tank.alive) minDist = Math.min(minDist, reaver.dist(tank))
    }

    (reaver, marine1, tank, marine2, world.frame, minDist / 32.0)
  }

  test("a plain hunter clears its side, walks at the tank and dies", StarEngineTags.Reaver) {
    val (reaver, marine1, tank, _, frames, minTiles) = runReaver(new HuntAllPilot)
    println(s"DIAG hunt frames=$frames reaver=${reaver.hp.toInt} marine1=${marine1.hp.toInt} tank=${tank.hp.toInt} minTiles=$minTiles")
    assert( ! marine1.alive, "hunter never cleared its side")
    assert( ! reaver.alive, "tank-hunting reaver survived - no problem to fix")
    assert(tank.alive, "reaver traded up into the gun it must never approach")
  }

  test("a keep-off hunter clears its side, holds the line and lives", StarEngineTags.Reaver) {
    val pilot = new HuntOthersPilot
    val (reaver, marine1, tank, _, frames, minTiles) = runReaver(pilot)
    println(s"DIAG keepline frames=$frames yields=${pilot.yields} reaver=${reaver.hp.toInt} marine1=${marine1.hp.toInt} minTiles=$minTiles")
    assert( ! marine1.alive, "keep-off hunter stopped hunting other units")
    assert(pilot.yields > 0, "keep-off never fired on the road past a sieged tank")
    assert(reaver.alive, "keep-off reaver died outside the line")
    assert(minTiles >= 12.0, f"reaver entered shell reach: $minTiles%.1f tiles")
  }
}
