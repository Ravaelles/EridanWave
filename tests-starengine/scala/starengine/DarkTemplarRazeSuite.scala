package starengine

import Micro.Targeting.DarkTemplarTargets
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Dark Templar target priority, real numbers: a DT with no cooldown kills
 * the weakest in reach first (a 35hp Zergling dies to one swipe), then
 * tanks, then warriors, then workers, nearest inside each tier. The pilot
 * calls the shipped kernel, so the kill order proves the shipped rule,
 * not a copy: the Ling must die before the closer Marine.
 */
class DarkTemplarRazeSuite extends AnyFunSuite {

  private class TierDuel(fallback: (Double, Double)) extends Pilot {

    override def direct(me: Body, world: World): Directive = {
      val live = world.foes.filter(_.alive)
      if (live.isEmpty) return MoveTo(fallback._1, fallback._2)

      if (me.cooldownLeft <= 0) {
        val near = live.filter(f => me.canHit(f) && me.dist(f) <= DarkTemplarTargets.closeTiles * 32.0)
        if (near.nonEmpty) {
          val marks = near.map(f => DarkTemplarTargets.Mark(
            me.dist(f) / 32.0,
            f.hp.toInt,
            f.kind == Terran.SiegeTankSieged || f.kind == Terran.SiegeTankUnsieged,
            f.kind.isWarrior,
            f.kind.isWorker))
          DarkTemplarTargets.pick(marks) match {
            case Some(i) => return Chase(near(i))
            case None    =>
          }
        }
      }

      live.minBy(me.dist) match {
        case foe => Chase(foe)
      }
    }
  }

  test("a fresh Templar swipes the weak before the close") {
    val ling = new Body(Zerg.Zergling, 0.0, 288.0, Zerg.Zergling.maxTotalHealth)
    val marine = new Body(Terran.Marine, 0.0, 292.0, Terran.Marine.maxTotalHealth)
    val scv = new Body(Terran.SCV, 0.0, 330.0, Terran.SCV.maxTotalHealth)
    val templar = new Body(Protoss.DarkTemplar, 0.0, 300.0, Protoss.DarkTemplar.maxTotalHealth)
    val world = new World(
      Seq(templar),
      Seq(ling, marine, scv),
      Map(
        templar -> new TierDuel((0.0, 0.0)),
        ling -> new Pilots.Hold,
        marine -> new Pilots.Hold,
        scv -> new Pilots.Hold),
      Seq.empty)

    var lingDeadFrame: Option[Int] = None
    var marineDeadFrame: Option[Int] = None
    var scvDeadFrame: Option[Int] = None
    while (world.frame < 2000 && (ling.alive || marine.alive || scv.alive) && templar.alive) {
      world.step()
      if (lingDeadFrame.isEmpty && ! ling.alive) lingDeadFrame = Some(world.frame)
      if (marineDeadFrame.isEmpty && ! marine.alive) marineDeadFrame = Some(world.frame)
      if (scvDeadFrame.isEmpty && ! scv.alive) scvDeadFrame = Some(world.frame)
    }

    assert(lingDeadFrame.isDefined, "templar never swiped the weak ling in reach")
    assert(marineDeadFrame.isDefined, "templar never dueled the marine")
    assert(scvDeadFrame.isDefined, "templar never finished the worker")
    assert(lingDeadFrame.get < marineDeadFrame.get, "templar shot the closer marine before the weaker ling")
    assert(marineDeadFrame.get < scvDeadFrame.get, "templar shot the worker before the warrior")
    assert(templar.alive, "templar died to unanswered miners")
  }
}
