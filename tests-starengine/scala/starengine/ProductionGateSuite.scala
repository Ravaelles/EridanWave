package starengine

import Gameplans.Protoss.PvP.DragoonsBeforeReavers
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite
import starengine.production.{Production, ProductionOrder}

/**
 * Policy-veto emergence: the shipped gate shape (backbone before the
 * first hull) composed with the production harness. A standing dragoon
 * pump fills the backbone on the gateway while the Robo orders reavers
 * only through the gate, reading the live emerged count every frame -
 * no Reaver starts before fifteen Dragoons emerge, then the flow
 * opens; below the backbone the Robo stays silent; a known Cannon
 * answers without the count. Table numbers only.
 */
class ProductionGateSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Production.name) }

  private val dragoonBuildFrames: Int = Protoss.Dragoon.buildFrames
  private val reaverBuildFrames: Int = Protoss.Reaver.buildFrames

  private def liveDragoons(production: Production, world: World): Int =
    world.ours.count(b => b.alive && b.kind == Protoss.Dragoon) +
      production.emerged.count(b => b.alive && b.kind == Protoss.Dragoon)

  /** Standing dragoon pump on the gateway. */
  private class DragoonProducer(production: Production, gateway: Body) extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      production.order(gateway, ProductionOrder.Pump(Protoss.Dragoon, maxTotal = 99))
      Hold
    }
  }

  /** Reaver pump on the Robo, issued only through the shipped gate. */
  private class GatedReaverProducer(
    production: Production,
    robo: Body,
    cannonKnown: Boolean) extends Pilot {

    override def direct(me: Body, world: World): Directive = {
      if (DragoonsBeforeReavers.allowReavers(liveDragoons(production, world), cannonKnown))
        production.order(robo, ProductionOrder.Pump(Protoss.Reaver, maxTotal = 99))
      Hold
    }
  }

  private def worldWith(
    production: Production,
    extraOurs: Seq[Body],
    cannonKnown: Boolean,
    pumpDragoons: Boolean = true): (World, Body, Body) = {
    val gateway = new Body(Protoss.Gateway, 500.0, 500.0, Protoss.Gateway.maxTotalHealth)
    val robo = new Body(Protoss.RoboticsFacility, 620.0, 500.0, Protoss.RoboticsFacility.maxTotalHealth)
    val pylon = new Body(Protoss.Pylon, 560.0, 560.0, Protoss.Pylon.maxTotalHealth)
    val ours = Seq(gateway, robo, pylon) ++ extraOurs
    val pilots: Map[Body, Pilot] =
      if (pumpDragoons) Map(
        gateway -> new DragoonProducer(production, gateway),
        robo -> new GatedReaverProducer(production, robo, cannonKnown))
      else Map(robo -> new GatedReaverProducer(production, robo, cannonKnown))
    val world = new World(
      ours,
      Seq.empty,
      pilots,
      Seq(new Constraint {
        override def apply(world: World): Unit = production.step(world, world.ours)
      }))
    (world, gateway, robo)
  }

  test("no Reaver starts before fifteen Dragoons emerge, then the flow opens") {
    val production = new Production(minerals = 40000, gas = 40000)
    val (world, _, _) = worldWith(production, Seq.empty, cannonKnown = false)
    var guard = 40 * dragoonBuildFrames
    while (production.emerged.count(_.kind == Protoss.Dragoon) < 15 && guard > 0) {
      world.step()
      guard -= 1
      assert(production.started(Protoss.Reaver) == 0, "backbone first: no hull starts early")
    }
    assert(production.emerged.count(_.kind == Protoss.Dragoon) >= 15, "the backbone arrives")
    var flow = 3 * reaverBuildFrames
    while (production.emerged.count(_.kind == Protoss.Reaver) < 1 && flow > 0) {
      world.step()
      flow -= 1
    }
    assert(production.emerged.count(_.kind == Protoss.Reaver) == 1, "open gate flows at table rate")
  }

  test("fourteen Dragoons keep the Robo silent") {
    val production = new Production(minerals = 40000, gas = 40000)
    val pack = (0 until 14).map(i => new Body(Protoss.Dragoon, 100.0 + i * 40.0, 900.0, Protoss.Dragoon.maxTotalHealth))
    // No dragoon pump here: the count must freeze at fourteen, so the
    // gate never opens mid-run the way a live backbone would open it.
    val (world, _, _) = worldWith(production, pack, cannonKnown = false, pumpDragoons = false)
    world.run(3 * reaverBuildFrames)
    assert(production.started(Protoss.Reaver) == 0, "closed gate issues nothing")
    assert(production.minerals == 40000 && production.gas == 40000, "silence spends nothing")
  }

  test("a known Cannon answers without the backbone") {
    val production = new Production(minerals = 40000, gas = 40000)
    val (world, _, _) = worldWith(production, Seq.empty, cannonKnown = true)
    world.run(reaverBuildFrames + 10)
    assert(production.emerged.count(_.kind == Protoss.Reaver) == 1, "the exception flows")
  }
}
