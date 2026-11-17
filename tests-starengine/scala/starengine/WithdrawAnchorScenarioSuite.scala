package starengine

import Tactic.Squads.Decisions.WithdrawAnchor
import org.scalatest.funsuite.AnyFunSuite

/**
 * Withdrawal anchor: a withdrawing cluster reforms at the middle choke
 * instead of bleeding home through the open map - the choke narrows what
 * can chase, and the march back out is short. Home when the choke is
 * unknown or the ground path there does not exist (sealed in means
 * reforming inside, not walking into the seal). Both pilots call the
 * shipped kernel with the world's path answer; the rects are the only
 * terrain the engine models.
 */
class WithdrawAnchorScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Retreat.name) }

  private class AnchorPilot(home: (Double, Double), choke: Option[(Double, Double)]) extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val reaches = choke.exists(c => world.groundPathExists(me.x, me.y, c._1, c._2))
      val (ax, ay) = WithdrawAnchor.anchor(home._1, home._2, choke, reaches)
      MoveTo(ax, ay)
    }
  }

  private def runAnchor(rects: Seq[(Double, Double, Double, Double)], frames: Int): (Double, Double) = {
    import ProxyBwapi.Races.Protoss
    val unit = new Body(Protoss.Dragoon, 0.0, -300.0, Protoss.Dragoon.maxTotalHealth)
    val home = (0.0, -600.0)
    val choke = Some((0.0, 0.0))
    val world = new World(Seq(unit), Seq.empty, Map(unit -> new AnchorPilot(home, choke)), Seq.empty)
    world.blockedRects = rects
    var f = 0
    while (f < frames) { world.step(); f += 1 }
    (unit.x, unit.y)
  }

  test("an open middle choke anchors the withdrawal") {
    val (x, y) = runAnchor(Seq.empty, 200)
    println(s"DIAG open anchor=(${x.toInt},${y.toInt})")
    assert(Math.hypot(x, y) < 64.0, "the withdrawal did not reform at the choke")
  }

  test("a sealed middle choke falls back home") {
    val (x, y) = runAnchor(Seq((-200.0, -150.0, 200.0, -50.0)), 300)
    println(s"DIAG sealed anchor=(${x.toInt},${y.toInt})")
    assert(Math.hypot(x, y + 600.0) < 64.0, "the withdrawal walked into the seal instead of home")
  }
}
