package starengine

import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite
import starengine.production.{Production, ProductionOrder}

/**
 * Production parity: the harness must reproduce today's request
 * semantics before the policy sink is built on top of it. A funded,
 * free, powered gateway pumps at table rate; a busy one queues instead
 * of overlapping; unpowered and broke structures wait in silence; a
 * `BuildOnce` emits exactly one hull ever; and only the right
 * structure trains the kind - a Pylon trains nothing. Table numbers
 * only: costs and build frames come from the unit tables, never from
 * the suite.
 */
class ProductionParitySuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Production.name) }

  private val dragoonBuildFrames: Int = Protoss.Dragoon.buildFrames
  private val dragoonMinerals: Int = Protoss.Dragoon.mineralPrice
  private val dragoonGas: Int = Protoss.Dragoon.gasPrice

  /** Producer pilot: re-issues one standing order, holds the structure. */
  private class Producer(
    production: Production,
    structure: Body,
    standing: ProductionOrder.Standing) extends Pilot {

    override def direct(me: Body, world: World): Directive = {
      production.order(structure, standing)
      Hold
    }
  }

  private def poweredBase(production: Production, order: ProductionOrder.Standing): (World, Body, Seq[Body]) = {
    val gateway = new Body(Protoss.Gateway, 500.0, 500.0, Protoss.Gateway.maxTotalHealth)
    val pylon = new Body(Protoss.Pylon, 560.0, 500.0, Protoss.Pylon.maxTotalHealth)
    val ours = Seq(gateway, pylon)
    val world = new World(
      ours,
      Seq.empty,
      Map(gateway -> new Producer(production, gateway, order)),
      Seq(new Constraint {
        override def apply(world: World): Unit = production.step(world, world.ours)
      }))
    (world, gateway, ours)
  }

  test("a funded powered free gateway pumps dragoons at table rate") {
    val production = new Production(minerals = 10000, gas = 10000)
    val (world, gateway, _) =
      poweredBase(production, ProductionOrder.Pump(Protoss.Dragoon, maxTotal = 99))
    world.run(3 * dragoonBuildFrames + 10)
    assert(production.emerged.size == 3, "one hull per build cycle, no more")
    assert(production.emerged.forall(b => b.kind == Protoss.Dragoon && b.hp == Protoss.Dragoon.maxTotalHealth))
    assert(production.emerged.forall(b => b.x == gateway.x && b.y == gateway.y), "hulls emerge at the gate")
    assert(production.minerals == 10000 - 4 * dragoonMinerals, "three out, fourth paid and training")
    assert(production.gas == 10000 - 4 * dragoonGas)
    assert(production.busyLeft(gateway) > 0, "the fourth trains on")
  }

  test("a busy gateway queues instead of overlapping") {
    val production = new Production(minerals = 10000, gas = 10000)
    val (world, gateway, _) =
      poweredBase(production, ProductionOrder.Pump(Protoss.Dragoon, maxTotal = 99))
    world.run(2 * dragoonBuildFrames + 10)
    assert(production.emerged.size == 2, "no second start inside a busy cycle")
    assert(production.minerals == 10000 - 3 * dragoonMinerals, "one start per cycle, never two")
  }

  test("an unpowered gateway emits nothing") {
    val production = new Production(minerals = 10000, gas = 10000)
    val gateway = new Body(Protoss.Gateway, 500.0, 500.0, Protoss.Gateway.maxTotalHealth)
    val world = new World(
      Seq(gateway),
      Seq.empty,
      Map(gateway -> new Producer(production, gateway, ProductionOrder.Pump(Protoss.Dragoon, maxTotal = 99))),
      Seq(new Constraint {
        override def apply(world: World): Unit = production.step(world, world.ours)
      }))
    world.run(2 * dragoonBuildFrames)
    assert(production.emerged.isEmpty, "no power, no hulls")
    assert(production.minerals == 10000 && production.gas == 10000, "no power, no spend")
  }

  test("a broke gateway waits, then resumes when funded") {
    val production = new Production(minerals = 0, gas = 0)
    val (world, _, _) =
      poweredBase(production, ProductionOrder.Pump(Protoss.Dragoon, maxTotal = 99))
    world.run(dragoonBuildFrames)
    assert(production.emerged.isEmpty, "broke means silent")
    assert(production.minerals == 0, "never spends what it has not got")
    production.minerals = dragoonMinerals
    production.gas = dragoonGas
    world.run(dragoonBuildFrames + 10)
    assert(production.emerged.size == 1, "funding resumes the same order")
  }

  test("a BuildOnce emits exactly one hull ever") {
    val production = new Production(minerals = 10000, gas = 10000)
    val (world, _, _) =
      poweredBase(production, ProductionOrder.Once(Protoss.Dragoon, count = 1))
    world.run(4 * dragoonBuildFrames)
    assert(production.emerged.size == 1, "one hull, then silence forever")
    assert(production.minerals == 10000 - dragoonMinerals, "paid exactly once")
  }

  test("a Pylon trains no Dragoons") {
    val production = new Production(minerals = 10000, gas = 10000)
    val pylon = new Body(Protoss.Pylon, 560.0, 500.0, Protoss.Pylon.maxTotalHealth)
    val world = new World(
      Seq(pylon),
      Seq.empty,
      Map(pylon -> new Producer(production, pylon, ProductionOrder.Pump(Protoss.Dragoon, maxTotal = 99))),
      Seq(new Constraint {
        override def apply(world: World): Unit = production.step(world, world.ours)
      }))
    world.run(2 * dragoonBuildFrames)
    assert(production.emerged.isEmpty, "wrong structure, no hulls")
    assert(production.minerals == 10000, "wrong structure, no spend")
  }
}
