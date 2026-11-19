package starengine

import Micro.Targeting.DarkTemplarHunt
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Main-base DT hunt, step by step: our Dragoon plus an Observer face
 * two cloaked enemy Templars, one nine tiles from our main and one
 * twenty-eight, both revealed by the Observer. The pilot calls the
 * shipped DarkTemplarHunt ladder on marks built from the live bodies
 * and commits to the pick: nearest to the main dies first, the farther
 * holds untouched until then. Speeds, reaches, damage and the 40-tile
 * hunt gate are the real numbers.
 */
class DarkTemplarHuntSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.DT.name + StarEngineTags.Targeting.name + StarEngineTags.Hunt.name) }

  private val mainPx = (0.0, 0.0)

  private def markOf(me: Body, foe: Body): DarkTemplarHunt.Mark =
    DarkTemplarHunt.Mark(Math.hypot(foe.x - mainPx._1, foe.y - mainPx._2) / 32.0)

  private class HuntPilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val revealed = world.foes.filter(f =>
        f.alive && f.kind == Protoss.DarkTemplar && world.detectorsRevealing(f).nonEmpty)
      val warriors = world.ours.count(o => o.alive && o.kind.isWarrior)
      val inRange = revealed.filter(f =>
        DarkTemplarHunt.inHuntRange(Math.hypot(f.x - mainPx._1, f.y - mainPx._2) / 32.0, warriors))
      DarkTemplarHunt.pick(inRange.map(markOf(me, _))).map(i => Chase(inRange(i))).getOrElse(Hold)
    }
  }

  test("nearest to the main takes fire while the farther holds untouched") {
    val dragoon = new Body(Protoss.Dragoon, 150.0, 100.0, Protoss.Dragoon.maxTotalHealth)
    val eye = new Body(Protoss.Observer, 500.0, 50.0, Protoss.Observer.maxTotalHealth)
    val near = new Body(Protoss.DarkTemplar, 300.0, 0.0, Protoss.DarkTemplar.maxTotalHealth)
    val far = new Body(Protoss.DarkTemplar, 700.0, 100.0, Protoss.DarkTemplar.maxTotalHealth)
    near.cloaked = true
    far.cloaked = true
    val pilot = new HuntPilot
    val world = new World(Seq(dragoon, eye), Seq(near, far),
      Map(dragoon -> pilot, eye -> new Pilots.Hold, near -> new Pilots.Hold, far -> new Pilots.Hold), Seq.empty)

    var firstHit: Option[Body] = None
    while (world.frame < 1000 && firstHit.isEmpty && dragoon.alive) {
      world.step()
      firstHit = Seq(near, far).find(b => b.hp < b.maxHp)
    }

    println(s"DIAG hunt frames=${world.frame} near=${near.hp.toInt} far=${far.hp.toInt} dragoon=${dragoon.hp.toInt}")
    assert(firstHit.isDefined, "nobody took fire - the pick never committed")
    assert(firstHit.contains(near), "the farther Templar took fire before the nearer one")
    assert(far.hp == far.maxHp, "the farther Templar took fire while the nearer stood")
  }
}
