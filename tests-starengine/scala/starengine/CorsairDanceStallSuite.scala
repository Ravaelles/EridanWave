package starengine

import ProxyBwapi.Races.{Protoss, Zerg}
import Tactic.Corsair.CorsairDanceToOverlord
import Utilities.Latch
import org.scalatest.funsuite.AnyFunSuite

/**
 * Corsair dance latch, regression guard: the latched standoff from the
 * Crona flap must not break the working duel. One Corsair runs the
 * production rule shape (shipped shouldDance behind a shipped Latch:
 * fires on the dance edge, releases on a ready gun or a dead duel)
 * against a drifting Overlord in the open field; it must close and
 * kill like the unlatched rule does. The flap itself lives in
 * per-frame target alternation the engine does not host - the live
 * PvZ hunt (Hunt/Dance alternation rate in the log) proves the fix.
 */
class CorsairDanceStallSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Corsair.name) }

  private class DriftPilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val foe = world.ours.find(_.alive).get
      val dx = me.x - foe.x
      val dy = me.y - foe.y
      val dist = Math.hypot(dx, dy)
      if (dist > 0) MoveTo(me.x + dx / dist * 200.0, me.y + dy / dist * 200.0)
      else Hold
    }
  }

  private class LatchedCorsair extends Pilot {
    val latched = new Latch
    var flips: Int = 0
    var holding: Boolean = false

    override def direct(me: Body, world: World): Directive = {
      val foe = world.foes.find(_.alive)
      if (foe.isEmpty) return Hold

      val distTiles = me.dist(foe.get) / 32.0
      val duelingNow = me.cooldownLeft > 0
      val dance = latched(
        1,
        fires    = duelingNow && CorsairDanceToOverlord.shouldDance(me.cooldownLeft, distTiles, 0.0, sporeCover = true),
        releases = me.cooldownLeft == 0 || ! duelingNow)
      if (dance != holding) flips += 1
      holding = dance
      if (dance) Hold else Chase(foe.get)
    }
  }

  test("the latched standoff still closes and kills in the open field") {
    val corsair = new Body(Protoss.Corsair, 0.0, 500.0, Protoss.Corsair.maxTotalHealth)
    val overlord = new Body(Zerg.Overlord, 7.0 * 32.0, 500.0, Zerg.Overlord.maxTotalHealth)
    val pilot = new LatchedCorsair
    val world = new World(Seq(corsair), Seq(overlord),
      Map(corsair -> pilot, overlord -> new DriftPilot), Seq.empty)
    while (world.frame < 3000 && corsair.alive && overlord.alive) world.step()
    println(s"DIAG flips=${pilot.flips} frames=${world.frame} overlord=${overlord.hp.toInt}")
    assert( ! overlord.alive, "latched duel never killed the drifting overlord")
  }
}
