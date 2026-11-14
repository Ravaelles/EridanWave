package starengine

import Micro.Actions.Protoss.ReaverShuttle.ReaverShuttleFlight
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Ferry vs flee: a dueling ride ferries at the enemy while dragoons
 * breathe across the preemptive edge. The ferry destination (at the
 * enemy) directly opposes the flee destination (away), so a sharp edge
 * flips every crossing and the ride never delivers - a UMS duel shows
 * AirFlee/DuelFerry alternating ~100 times each in the losing run
 * against identical starting positions. The latched ride keys on the
 * worst penetration across the pack (one call per frame, so a second
 * threat cannot overwrite the hold) and releases two tiles past every
 * trigger. Speeds, reaches and margins are the real UnitType numbers;
 * both pilots call the shipped kernels, the engine owns the loop only.
 */
class ShuttleFerryLatchSuite extends AnyFunSuite {

  private class FerryPilot(id: Int, latched: Boolean) extends Pilot {
    val fled = new scala.collection.mutable.ListBuffer[Boolean]()

    override def direct(me: Body, world: World): Directive = {
      val foes = world.foes.filter(_.alive)
      val scored = foes.map(f =>
        (f.kind.pixelRangeGround + 4.0 * 32.0 - me.dist(f), f))
      val worst = scored.reduceOption((a, b) => if (a._1 >= b._1) a else b)
      val fleeNow =
        if (latched) ReaverShuttleFlight.fleeLatched(id, worst.map(_._1).getOrElse(Double.NegativeInfinity))
        else worst.exists(_._1 > 0)
      fled += fleeNow
      if (fleeNow) {
        val focus = worst.map(_._2).getOrElse(me)
        val dx = me.x - focus.x
        val dy = me.y - focus.y
        val dist = Math.sqrt(dx * dx + dy * dy)
        if (dist > 0) MoveTo(me.x + dx / dist * 256.0, me.y + dy / dist * 256.0) else Hold
      } else {
        MoveTo(400.0, 300.0)
      }
    }

    def maxRun: Int = {
      var best = 0
      var cur = 0
      var prev: Option[Boolean] = None
      fled.foreach(f => {
        if (prev.contains(f)) cur += 1 else cur = 1
        prev = Some(f)
        if (cur > best) best = cur
      })

      best
    }
  }

  private class BreathingDuo extends Pilot {
    override def direct(me: Body, world: World): Directive =
      MoveTo(me.x, 444.0 + 96.0 * Math.sin(world.frame / 60.0))
  }

  private case class Outcome(flips: Int, maxRun: Int, closestGoal: Double)

  private def runFerry(latched: Boolean): Outcome = {
    if (latched) ReaverShuttleFlight.reset()
    val ride = new Body(Protoss.Shuttle, 400.0, 700.0, Protoss.Shuttle.maxTotalHealth)
    val dragoon1 = new Body(Protoss.Dragoon, 380.0, 444.0, Protoss.Dragoon.maxTotalHealth)
    val dragoon2 = new Body(Protoss.Dragoon, 420.0, 444.0, Protoss.Dragoon.maxTotalHealth)
    val pilot = new FerryPilot(1, latched)
    val pilots: Map[Body, Pilot] = Map(
      ride -> pilot,
      dragoon1 -> new BreathingDuo,
      dragoon2 -> new BreathingDuo)
    val world = new World(Seq(ride), Seq(dragoon1, dragoon2), pilots, Seq.empty)

    var closest = Double.PositiveInfinity
    while (world.frame < 1500 && ride.alive) {
      world.step()
      closest = Math.min(closest, Math.hypot(ride.x - 400.0, ride.y - 300.0))
    }
    val flips = pilot.fled.sliding(2).count(w => w(0) != w(1))
    Outcome(flips, pilot.maxRun, closest)
  }

  test("a sharp edge flips ferry and flee every breath") {
    val ride = new Body(Protoss.Shuttle, 0.0, 0.0, 1.0)
    println(s"DIAG ride speed=${ride.speed}")
    val sharp = runFerry(latched = false)
    println(s"DIAG sharp flips=${sharp.flips} maxRun=${sharp.maxRun}f closest=${sharp.closestGoal.toInt}px")
    assert(sharp.flips >= 400, s"no flap to fix: ${sharp.flips} flips")
    assert(sharp.maxRun <= 12, s"sharp held a course: ${sharp.maxRun}f")
  }

  test("a latched ferry holds through the breathing") {
    val fixed = runFerry(latched = true)
    println(s"DIAG fixed flips=${fixed.flips} maxRun=${fixed.maxRun}f closest=${fixed.closestGoal.toInt}px")
    assert(fixed.flips <= 120, s"still flapping: ${fixed.flips} flips")
    assert(fixed.maxRun >= 80, s"never settled: longest run ${fixed.maxRun}f")
  }
}
