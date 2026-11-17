package starengine

import Micro.Targeting.FiltersRequired.TargetFilterScout
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Scout discipline, step by step: a lone Protoss Scout finds a Carrier
 * screened by Interceptors, and a wingmate pair finds the same. The
 * pilots call the shipped TargetFilterScout vote on the same facts
 * production reads (scout hull, Protoss mirror, Carrier victim,
 * wingmate inside five tiles) and only the escorted hull closes -
 * Interceptors never pass, lone hulls hold the line instead of feeding
 * a Carrier. Speeds, reaches and damage are the real UnitType numbers.
 */
class ScoutDisciplineScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Scout.name + StarEngineTags.Targeting.name + StarEngineTags.Carrier.name) }

  /** Pilot, production order: the shipped filter votes, legs execute. */
  private class ScoutPilot extends Pilot {
    var chases = 0

    override def direct(me: Body, world: World): Directive = {
      val carriers = world.foes.filter(f => f.alive && f.kind == Protoss.Carrier)
      val mateNear = world.ours.exists(o => o != me && o.alive && o.kind == Protoss.Scout && me.dist(o) <= TargetFilterScout.wingmateTiles * 32.0)
      val legal = carriers.filter(c =>
        TargetFilterScout.allowsVictim(isScout = true, enemyIsProtoss = true, targetIsCarrier = true, mateNear))
      if (legal.isEmpty) return Hold

      chases += 1
      Chase(legal.minBy(me.dist))
    }
  }

  /** Wingmate glue: the escort holds formation on its lead. */
  private class GluePilot(lead: Body) extends Pilot {
    override def direct(me: Body, world: World): Directive =
      if (lead.alive) MoveTo(lead.x, lead.y) else Hold
  }

  test("a lone Scout holds the line instead of feeding the Carrier") {
    val scout = new Body(Protoss.Scout, 0.0, 500.0, Protoss.Scout.maxTotalHealth)
    val carrier = new Body(Protoss.Carrier, 400.0, 500.0, Protoss.Carrier.maxTotalHealth)
    val interceptor = new Body(Protoss.Interceptor, 430.0, 500.0, Protoss.Interceptor.maxTotalHealth)
    val pilot = new ScoutPilot
    val world = new World(Seq(scout), Seq(carrier, interceptor),
      Map(scout -> pilot, carrier -> new Pilots.Hold, interceptor -> new Pilots.Hold), Seq.empty)

    var f = 0
    while (f < 300 && scout.alive) {
      world.step()
      f = world.frame
    }
    println(s"DIAG lone chases=${pilot.chases} scout=${scout.hp} carrier=${carrier.hp}")
    assert(pilot.chases == 0, "lone Scout chased without a wingmate")
    assert(scout.alive, "lone Scout fed the Carrier")
  }

  test("an escorted Scout closes the Carrier past its screen") {
    val scout = new Body(Protoss.Scout, 0.0, 500.0, Protoss.Scout.maxTotalHealth)
    val mate = new Body(Protoss.Scout, 60.0, 500.0, Protoss.Scout.maxTotalHealth)
    val interceptor = new Body(Protoss.Interceptor, 200.0, 700.0, Protoss.Interceptor.maxTotalHealth)
    val carrier = new Body(Protoss.Carrier, 400.0, 500.0, Protoss.Carrier.maxTotalHealth)
    val pilot = new ScoutPilot
    val wingmate = new GluePilot(scout)
    val world = new World(Seq(scout, mate), Seq(interceptor, carrier),
      Map(scout -> pilot, mate -> wingmate, carrier -> new Pilots.Hold, interceptor -> new Pilots.Hold), Seq.empty)

    var f = 0
    while (f < 1000 && scout.alive && carrier.alive) {
      world.step()
      f = world.frame
    }
    println(s"DIAG escorted chases=${pilot.chases} scout=${scout.hp} carrier=${carrier.hp} interceptor=${interceptor.hp}")
    assert(pilot.chases > 0, "escorted Scout never closed")
    assert(carrier.hp < Protoss.Carrier.maxTotalHealth, "escorted Scout never hit the Carrier")
    assert(interceptor.hp == Protoss.Interceptor.maxTotalHealth, "escort chewed the screen instead of the Carrier")
  }
}
