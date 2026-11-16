package starengine

import Micro.Actions.Protoss.DarkTemplar.SpiderMineSniping
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Mine sidestep, step by step: a Dark Templar must cross a row of
 * known Spider Mines to reach the far side. The old walk goes
 * straight through the trigger radius and would eat the blast. The
 * sidestep pilot calls the shipped kernel: while mines sit inside six
 * tiles it steps laterally off their centroid (fixed side per hull,
 * no zigzag), otherwise it walks at the goal. Speeds and reaches are
 * the real UnitType numbers; the engine models no blast, so the proof
 * is the path: never inside the trigger radius, goal reached.
 */
class DarkTemplarMineSuite extends AnyFunSuite with StarEngineSuite {

  private class SidestepPilot(goal: (Double, Double)) extends Pilot {
    var steps = 0
    var minMineTiles = Double.PositiveInfinity
    // Fixed side for the repro; production picks by unit id parity.
    val goLeft = true

    override def direct(me: Body, world: World): Directive = {
      val mines = world.foes.filter(f => f.alive && f.kind == Terran.SpiderMine && me.dist(f) / 32.0 <= SpiderMineSniping.sidestepTiles)
      mines.foreach(m => minMineTiles = Math.min(minMineTiles, me.dist(m) / 32.0))
      if (mines.nonEmpty) {
        steps += 1
        val cx = mines.map(_.x).sum / mines.size
        val cy = mines.map(_.y).sum / mines.size
        val step = SpiderMineSniping.sidestepPixel(
          new Mathematics.Points.Pixel(me.x.toInt, me.y.toInt),
          new Mathematics.Points.Pixel(cx.toInt, cy.toInt),
          SpiderMineSniping.sidestepStepTiles,
          goLeft)
        MoveTo(step.x, step.y)
      } else {
        MoveTo(goal._1, goal._2)
      }
    }
  }

  test("a Templar slides around the mine row instead of through it") {
    val templar = new Body(Protoss.DarkTemplar, 0.0, 500.0, Protoss.DarkTemplar.maxTotalHealth)
    val mines = Seq(300.0, 340.0, 380.0).map(x =>
      new Body(Terran.SpiderMine, x, 500.0, Terran.SpiderMine.maxTotalHealth))
    val pilot = new SidestepPilot((700.0, 500.0))
    val world = new World(Seq(templar), mines,
      Map(templar -> pilot) ++ mines.map(_ -> new Pilots.Hold).toMap, Seq.empty)

    while (world.frame < 3000 && templar.alive && Math.abs(templar.x - 700.0) > 32.0) {
      world.step()
    }

    println(s"DIAG sidestep frames=${world.frame} steps=${pilot.steps} minMine=${pilot.minMineTiles} x=${templar.x.toInt}")
    assert(pilot.steps > 0, "sidestep never fired on a mine row")
    assert(pilot.minMineTiles > 3.0, f"hull entered the blast at ${pilot.minMineTiles}%.1f tiles")
    assert(Math.abs(templar.x - 700.0) <= 32.0, "hull never reached the far side")
  }
}
