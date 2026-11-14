package starengine

import Micro.Targeting.DarkTemplarTargets
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Dark Templar target priority, real numbers: with no cooldown the
 * Templar kills the weakest in reach first (a 35hp Zergling dies to one
 * swipe), then warriors, then workers - nearest inside each tier. The
 * pilot calls the shipped kernel, so the kill order proves the shipped
 * rule, not a copy: the SCV starts closest but dies last.
 */
class DarkTemplarRazeSuite extends AnyFunSuite with StarEngineSuite {

  private def tierPilot(fallback: (Double, Double)): Pilot = new Pilot {

    override def direct(me: Body, world: World): Directive = {
      val live = world.foes.filter(_.alive)
      if (live.isEmpty) return MoveTo(fallback._1, fallback._2)

      if (me.cooldownLeft <= 0) {
        val near = live.filter(f => me.canHit(f) && me.dist(f) <= DarkTemplarTargets.closeTiles * 32.0)
        if (near.nonEmpty) {
          val marks = near.map(f => DarkTemplarTargets.Mark(
            distTiles = me.dist(f) / 32.0,
            totalHealth = f.hp.toInt,
            isTank = f.kind == Terran.SiegeTankSieged || f.kind == Terran.SiegeTankUnsieged,
            isWarrior = f.kind.isWarrior,
            isWorker = f.kind.isWorker))
          DarkTemplarTargets.pick(marks) match {
            case Some(i) => return Chase(near(i))
            case None    =>
          }
        }
      }

      Chase(live.minBy(me.dist))
    }
  }

  test("a fresh Templar swipes the weak before the close") {
    val ling = new Body(Zerg.Zergling, 0.0, 288.0, Zerg.Zergling.maxTotalHealth)
    val zealot = new Body(Protoss.Zealot, 0.0, 290.0, Protoss.Zealot.maxTotalHealth)
    val scv = new Body(Terran.SCV, 0.0, 250.0, Terran.SCV.maxTotalHealth)
    val templar = new Body(Protoss.DarkTemplar, 0.0, 300.0, Protoss.DarkTemplar.maxTotalHealth)
    val world = new World(
      Seq(templar),
      Seq(ling, zealot, scv),
      Map(
        templar -> tierPilot((0.0, 0.0)),
        ling -> new Pilots.Hold,
        zealot -> new Pilots.Hold,
        scv -> new Pilots.Hold),
      Seq.empty)

    var lingDeadFrame: Option[Int] = None
    var zealotDeadFrame: Option[Int] = None
    var scvDeadFrame: Option[Int] = None
    while (world.frame < 2000 && (ling.alive || zealot.alive || scv.alive) && templar.alive) {
      world.step()
      if (lingDeadFrame.isEmpty && ! ling.alive) lingDeadFrame = Some(world.frame)
      if (zealotDeadFrame.isEmpty && ! zealot.alive) zealotDeadFrame = Some(world.frame)
      if (scvDeadFrame.isEmpty && ! scv.alive) scvDeadFrame = Some(world.frame)
    }

    assert(lingDeadFrame.isDefined, "templar never swiped the weak ling in reach")
    assert(zealotDeadFrame.isDefined, "templar never dueled the warrior")
    assert(scvDeadFrame.isDefined, "templar never finished the worker")
    assert(lingDeadFrame.get < zealotDeadFrame.get, "templar shot the warrior before the weaker ling")
    assert(zealotDeadFrame.get < scvDeadFrame.get, "templar shot the worker before the warrior")
    assert(templar.alive, "templar died to unanswered miners")
  }
}
