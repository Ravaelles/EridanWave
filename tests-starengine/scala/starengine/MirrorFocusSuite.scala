package starengine

import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Mirror focus, crossed seats: two identical dragoon lines meet, one
 * side sharing the weakest target, the other spreading nearest-first.
 * Both seatings run - the engine lets foes shoot first, so a win in
 * one seat proves nothing and a win in both proves the mechanism.
 * The shipped FocusFire gate needs four shooters for a shared target;
 * these rows measure what a three-hull line pays for spreading.
 */
class MirrorFocusSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Dragoon.name + StarEngineTags.Targeting.name + StarEngineTags.PvP.name) }

  /** Shared weakest: the line deletes one hull at a time. */
  private class FocusPilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val live = me match {
        case _ if world.ours.contains(me) => world.foes
        case _ => world.ours
      }
      live.filter(_.alive).sortBy(_.hp).headOption.map(Chase).getOrElse(Hold)
    }
  }

  /** Spread nearest: every hull picks its own duel. */
  private class SpreadPilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val live = me match {
        case _ if world.ours.contains(me) => world.foes
        case _ => world.ours
      }
      live.filter(_.alive).sortBy(me.dist).headOption.map(Chase).getOrElse(Hold)
    }
  }

  private def grind(oursPilot: Pilot, foePilot: Pilot): (Int, Int) = {
    val ours = (0 until 3).map(i => new Body(Protoss.Dragoon, i * 40.0, 900.0, Protoss.Dragoon.maxTotalHealth))
    val foes = (0 until 3).map(i => new Body(Protoss.Dragoon, i * 40.0, 100.0, Protoss.Dragoon.maxTotalHealth))
    val world = new World(
      ours, foes,
      (ours.map(_ -> oursPilot) ++ foes.map(_ -> foePilot)).toMap,
      Seq.empty)
    world.run(6000)
    (ours.count(_.alive), foes.count(_.alive))
  }

  test("focus trades up on spread: kills, not survivors") {
    // Foes shoot first, so a symmetric grind can wipe either line -
    // survivors measure the seating, not the doctrine. Kills do: the
    // focused line must deal more than it takes in both seatings.
    val (focusOurs, spreadFoes) = grind(new FocusPilot, new SpreadPilot)
    val (spreadOurs, focusFoes) = grind(new SpreadPilot, new FocusPilot)
    val focusKills = (3 - spreadFoes) + (3 - spreadOurs)
    val spreadKills = (3 - focusOurs) + (3 - focusFoes)
    println(s"DIAG focus-seat ours=$focusOurs/3 foes=$spreadFoes/3; spread-seat ours=$spreadOurs/3 foes=$focusFoes/3; kills focus=$focusKills spread=$spreadKills")
    assert(focusKills >= spreadKills, "a focused line must not trade down")
  }

  test("spread mirrors bleed each other: the control") {
    val (a, b) = grind(new SpreadPilot, new SpreadPilot)
    println(s"DIAG spread-control ours=$a/3 foes=$b/3")
    assert(a + b < 6, "a spread mirror must trade hulls, not stall")
  }
}
