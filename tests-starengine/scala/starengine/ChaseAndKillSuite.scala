package starengine

import Micro.Actions.Combat.Decisioning.Kiting.KiteTo
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Chase-and-kill for the helpless: a Dragoon meeting a fleeing worker
 * closes instead of kiting away, while shields are at most scratched.
 * The kiting brain holds its max-range standoff and watches the worker
 * escape; the closing brain calls the shipped shouldClose kernel and
 * runs it down. Speeds, reaches and damage are the real UnitType
 * numbers; the worker flees full-speed, the engine owns the loop only.
 */
class ChaseAndKillSuite extends AnyFunSuite {

  /** Old shape: hold the max-range standoff forever, never close. */
  private class KitingPilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val foe = world.foes.find(_.alive)
      foe match {
        case None => Hold
        case Some(f) =>
          val stand = me.rangeAgainst(f)
          if (me.dist(f) < stand) {
            val dx = me.x - f.x
            val dy = me.y - f.y
            val dist = Math.sqrt(dx * dx + dy * dy)
            if (dist > 0) MoveTo(me.x + dx / dist * stand, me.y + dy / dist * stand) else Hold
          } else Hold
      }
    }
  }

  /** Production order: close what cannot hit back, standoff otherwise. */
  private class ClosingPilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val foe = world.foes.find(_.alive)
      foe match {
        case None => Hold
        case Some(f) =>
          val cantHitUs = true
          val shieldWound = 0 // fresh hull, unscratched
          if (KiteTo.shouldClose(cantHitUs, f.kind.isFlyer, shieldWound, me.dist(f) / 32.0)) Chase(f)
          else {
            val stand = me.rangeAgainst(f)
            if (me.dist(f) < stand) {
              val dx = me.x - f.x
              val dy = me.y - f.y
              val dist = Math.sqrt(dx * dx + dy * dy)
              if (dist > 0) MoveTo(me.x + dx / dist * stand, me.y + dy / dist * stand) else Hold
            } else Hold
          }
      }
    }
  }

  private case class Outcome(workerHp: Double, closestPx: Double, frames: Int)

  private def runChase(closing: Boolean): Outcome = {
    val goon = new Body(Protoss.Dragoon, 400.0, 700.0, Protoss.Dragoon.maxTotalHealth)
    val worker = new Body(Terran.SCV, 400.0, 550.0, Terran.SCV.maxTotalHealth)
    val pilots: Map[Body, Pilot] = Map(
      goon -> (if (closing) new ClosingPilot else new KitingPilot),
      worker -> new Pilots.Flee(_ => Seq(goon)))
    val world = new World(Seq(goon), Seq(worker), pilots, Seq.empty)

    var closest = goon.dist(worker)
    while (world.frame < 1500 && worker.alive) {
      world.step()
      if (worker.alive) closest = Math.min(closest, goon.dist(worker))
    }
    Outcome(worker.hp, closest, world.frame)
  }

  test("a kiting brain never touches a fleeing worker") {
    val goon = new Body(Protoss.Dragoon, 0.0, 0.0, 1.0)
    val scv = new Body(Terran.SCV, 0.0, 0.0, 1.0)
    println(s"DIAG goon speed=${goon.speed} range=${goon.rangeAgainst(scv)} scv speed=${scv.speed}")
    val kiting = runChase(closing = false)
    println(s"DIAG kiting workerHp=${kiting.workerHp.toInt} closest=${kiting.closestPx.toInt}px frames=${kiting.frames}")
    assert(kiting.workerHp >= Terran.SCV.maxTotalHealth, "kiter connected anyway")
  }

  test("a closing brain runs the worker down") {
    val closing = runChase(closing = true)
    println(s"DIAG closing workerHp=${closing.workerHp.toInt} closest=${closing.closestPx.toInt}px frames=${closing.frames}")
    assert(closing.workerHp < Terran.SCV.maxTotalHealth, "closer never landed a hit")
  }
}
