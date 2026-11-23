package starengine

import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Visualizer snapshot contract: the window never drives a verdict, so
 * the only thing suites pin is the view model - every live body shows
 * up with its side, kind, hit points and current order, and movement
 * orders carry the destination the arrow points at. Headless-safe:
 * building the model never opens a window.
 */
class VisualizerSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Dragoon.name + StarEngineTags.Zealot.name + StarEngineTags.PvP.name) }

  private def worldWithOrders(): World = {
    val ours = new Body(Protoss.Dragoon, 0.0, 0.0, Protoss.Dragoon.maxTotalHealth)
    val foe = new Body(Protoss.Zealot, 200.0, 0.0, Protoss.Zealot.maxTotalHealth)
    val world = new World(
      Seq(ours),
      Seq(foe),
      Map(ours -> new Pilots.Hold, foe -> new Pilots.Hold),
      Seq.empty)
    world.step()
    world
  }

  test("the snapshot carries both sides with orders and hit points") {
    val snap = Visualizer.snapshot(worldWithOrders())
    assert(snap.bodies.size == 2)
    val dragoon = snap.bodies.find(_.ours).get
    val zealot = snap.bodies.find(! _.ours).get
    assert(dragoon.kind.contains("Dragoon"))
    assert(zealot.kind.contains("Zealot"))
    assert(dragoon.order == "Hold")
    assert(zealot.order == "Hold")
    assert(dragoon.arrowTo.isEmpty)
    assert(dragoon.hp == dragoon.maxHp)
    assert(dragoon.maxHp == Protoss.Dragoon.maxTotalHealth)
    assert(zealot.hp == zealot.maxHp)
  }

  test("move orders carry the arrow destination, chases the target") {
    val ours = new Body(Protoss.Dragoon, 0.0, 0.0, Protoss.Dragoon.maxTotalHealth)
    val foe = new Body(Protoss.Zealot, 300.0, 40.0, Protoss.Zealot.maxTotalHealth)
    val world = new World(
      Seq(ours),
      Seq(foe),
      Map(ours -> new Pilot {
        override def direct(me: Body, world: World): Directive = MoveTo(500.0, 600.0)
      }, foe -> new Pilots.Hold),
      Seq.empty)
    world.step()
    val snap = Visualizer.snapshot(world)
    val dragoon = snap.bodies.find(_.ours).get
    assert(dragoon.order == "Move")
    assert(dragoon.arrowTo.exists { case (x, y) => x == 500.0 && y == 600.0 })
  }

  test("dead bodies never reach the view") {
    val ours = new Body(Protoss.Dragoon, 0.0, 0.0, Protoss.Dragoon.maxTotalHealth)
    val foe = new Body(Protoss.Zealot, 100.0, 0.0, 1.0)
    foe.hp = 0.0
    assert( ! foe.alive)
    val world = new World(
      Seq(ours),
      Seq(foe),
      Map(ours -> new Pilots.Hold, foe -> new Pilots.Hold),
      Seq.empty)
    world.step()
    val snap = Visualizer.snapshot(world)
    assert(snap.bodies.size == 1)
    assert(snap.bodies.head.ours)
  }
}
