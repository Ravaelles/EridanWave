package starengine

import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Mixed-line focus: two zealots plus two dragoons meet the mirror -
 * the ZealDrag duel. Every hull picking its own nearest target
 * spreads four guns over four hulls; a shared weakest target deletes
 * one hull at a time. Crossed seats (the engine lets foes shoot
 * first), kills decide.
 */
class MixedDuelSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Dragoon.name + StarEngineTags.Zealot.name + StarEngineTags.Targeting.name + StarEngineTags.PvP.name) }

  private def foesOf(me: Body, world: World): Seq[Body] =
    if (world.ours.contains(me)) world.foes else world.ours

  /** Shared weakest: every hull, melee or ranged, walks the same victim down. */
  private class FocusPilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val live = foesOf(me, world).filter(_.alive)
      live.sortBy(_.hp).headOption.map(Chase).getOrElse(Hold)
    }
  }

  /** Spread nearest: every hull picks its own duel. */
  private class SpreadPilot extends Pilot {
    override def direct(me: Body, world: World): Directive =
      foesOf(me, world).filter(_.alive).sortBy(me.dist).headOption.map(Chase).getOrElse(Hold)
  }

  private def line(y: Double, ours: Boolean): Seq[Body] =
    Seq(
      new Body(Protoss.Zealot, -20.0, y, Protoss.Zealot.maxTotalHealth),
      new Body(Protoss.Zealot, 20.0, y, Protoss.Zealot.maxTotalHealth),
      new Body(Protoss.Dragoon, -60.0, y, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 60.0, y, Protoss.Dragoon.maxTotalHealth))

  private def grind(oursP: Body => Pilot, foeP: Body => Pilot): (Int, Int) = {
    val ours = line(900.0, ours = true)
    val foes = line(100.0, ours = false)
    val world = new World(ours, foes, (ours.map(o => o -> oursP(o)) ++ foes.map(f => f -> foeP(f))).toMap, Seq.empty)
    world.run(9000)
    (4 - foes.count(_.alive), 4 - ours.count(_.alive))
  }

  test("a focused mixed line trades up on a spread one") {
    val (focusKills, spreadKillsA) = grind(_ => new FocusPilot, _ => new SpreadPilot)
    val (spreadKills, focusKillsB) = grind(_ => new SpreadPilot, _ => new FocusPilot)
    println(s"DIAG focus-seat focusKills=$focusKills spreadKills=$spreadKillsA; spread-seat spreadKills=$spreadKills focusKills=$focusKillsB")
    assert(focusKills + focusKillsB >= spreadKills + spreadKillsA, "a focused mixed line must not trade down")
  }

  test("spread mirrors bleed each other: the control") {
    val (a, b) = grind(_ => new SpreadPilot, _ => new SpreadPilot)
    println(s"DIAG spread-control ours=$a/4 foes=$b/4")
    assert(a + b > 0, "a spread mirror must trade hulls, not stall")
  }
}
