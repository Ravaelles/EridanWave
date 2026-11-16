package starengine

import Micro.Actions.Protoss.ReaverShuttle.ReaverShuttleReboard
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Pair cycle harness: ferry, drop, clear, reboard, extract. A loaded
 * Shuttle ferries its Reaver across the map, drops it on a Sunken, the
 * hull clears the static and its Drones, Hydralisks press in to defend,
 * the hull reboards under the shipped reboard band and the ride extracts
 * it. Pilots mirror the production claims: the ride escorts its mate at
 * board reach off the threat (never farther), drops on the goal, flees
 * only what can hit the hull; the hull fights the nearest foe and
 * boards pressured with the ride at hand. The engine supplies
 * Board/Unload, cargo and the ride-along sync. Green means the cycle
 * mechanics hold together; the dither repros (fetch/escort/await,
 * dropped targets) build on this harness next.
 */
class PairCycleSuite extends AnyFunSuite with StarEngineSuite {

  private val home = (0.0, 500.0)
  private val goal = (1700.0, 500.0)

  private implicit class BodyAt(b: Body) {
    def distTo(p: (Double, Double)): Double = {
      val dx = b.x - p._1
      val dy = b.y - p._2
      Math.sqrt(dx * dx + dy * dy)
    }
  }

  /** Escort ride: glued to the mate at board reach off the threat. */
  private class EscortPilot extends Pilot {
    var delivered = false
    var extracted = false

    override def direct(me: Body, world: World): Directive = {
      val mate = world.ours.find(o => o.alive && o.kind == Protoss.Reaver)
      if (mate.exists(! _.loaded)) delivered = true
      val threat = world.foes.filter(f => f.alive && f.canHit(me)).sortBy(me.dist).headOption
      val danger = threat.exists(t => me.dist(t) <= t.rangeAgainst(me) + 4.0 * 32.0)
      (mate, danger) match {
        case (Some(m), _) if m.loaded && delivered =>
          extracted = true

          MoveTo(home._1, home._2)
        case (Some(m), _) if m.loaded =>
          if (me.distTo(goal) > 64.0 && ! delivered) MoveTo(goal._1, goal._2)
          else Hold
        case (Some(m), true) =>
          val t = threat.get
          var dx = m.x - t.x
          var dy = m.y - t.y
          val dist = Math.sqrt(dx * dx + dy * dy)
          if (dist > 0) { dx /= dist; dy /= dist }
          MoveTo(m.x + dx * 80.0, m.y + dy * 80.0)
        case (Some(m), _) if m.dist(me) > 64.0 =>
          MoveTo(m.x, m.y)
        case _ =>
          Hold
      }
    }
  }

  /** Drop fighter: unloads on the goal, fights the nearest foe, boards pressured. */
  private class DropPilot(dropAt: (Double, Double)) extends Pilot {
    var boards = 0

    override def direct(me: Body, world: World): Directive = {
      val ride = world.ours.find(o => o.alive && o.kind == Protoss.Shuttle)
      if (me.loaded) {
        ride match {
          case Some(r) if Math.hypot(r.x - dropAt._1, r.y - dropAt._2) <= 256.0 => Unload
          case _ => Hold
        }
      } else {
        val threat = world.foes.filter(f => f.alive && f.canHit(me)).sortBy(me.dist).headOption
        val band = threat.exists(t =>
          ReaverShuttleReboard.mayReboard(me.dist(t) / 32.0, me.lossFraction))
        (ride.filter(r => me.dist(r) <= 64.0 && band), threat) match {
          case (Some(r), _) =>
            boards += 1

            Board(r)
          case _ =>
            world.nearestFoeOf(me, world.foes, (m, f) => m.canHit(f)) match {
              case Some(foe) => Chase(foe)
              // Nothing left to hunt: board the ride for extraction.
              case None => ride match {
                case Some(r) if me.dist(r) <= 64.0 =>
                  boards += 1

                  Board(r)
                case Some(r) => MoveTo(r.x, r.y)
                case None    => Hold
              }
            }
        }
      }
    }
  }

  /** Garrison: holds ground until the wake frame, then presses the nearest foe. */
  private class GarrisonPilot(fallback: (Double, Double), wakeFrame: Int) extends Pilot {

    override def direct(me: Body, world: World): Directive =
      if (world.frame < wakeFrame) Hold
      else Pilots.ZombieAttack.pick(me, world.ours) match {
        case Some(foe) => Chase(foe)
        case None      => MoveTo(fallback._1, fallback._2)
      }
  }

  test("ferry, drop, clear, reboard, extract") {
    val shuttle = new Body(Protoss.Shuttle, home._1, home._2, Protoss.Shuttle.maxTotalHealth)
    val reaver = new Body(Protoss.Reaver, home._1, home._2, Protoss.Reaver.maxTotalHealth)
    reaver.transport = Some(shuttle)
    val sunken = new Body(Zerg.SunkenColony, 2000.0, 500.0, Zerg.SunkenColony.maxTotalHealth)
    val drone1 = new Body(Zerg.Drone, 2200.0, 500.0, Zerg.Drone.maxTotalHealth)
    val drone2 = new Body(Zerg.Drone, 2250.0, 540.0, Zerg.Drone.maxTotalHealth)
    val hydra1 = new Body(Zerg.Hydralisk, 2600.0, 500.0, Zerg.Hydralisk.maxTotalHealth)
    val hydra2 = new Body(Zerg.Hydralisk, 2650.0, 540.0, Zerg.Hydralisk.maxTotalHealth)
    val escort = new EscortPilot
    val drop = new DropPilot(goal)
    val pilots: Map[Body, Pilot] = Map(
      shuttle -> escort,
      reaver -> drop,
      sunken -> new Pilots.Hold,
      drone1 -> new Pilots.Hold,
      drone2 -> new Pilots.Hold,
      hydra1 -> new GarrisonPilot((2000.0, 500.0), 900),
      hydra2 -> new GarrisonPilot((2000.0, 500.0), 900))
    val world = new World(Seq(shuttle, reaver), Seq(sunken, drone1, drone2, hydra1, hydra2), pilots, Seq.empty)

    while (world.frame < 12000 && reaver.alive && shuttle.alive
      && (sunken.alive || drone1.alive || drone2.alive || reaver.transport.isEmpty)) {
      world.step()
    }

    println(s"DIAG cycle frames=${world.frame} delivered=${escort.delivered} boards=${drop.boards} extracted=${escort.extracted} " +
      s"reaver=${reaver.hp.toInt} shuttle=${shuttle.hp.toInt} sunken=${sunken.hp.toInt} " +
      s"drones=${drone1.hp.toInt},${drone2.hp.toInt} hydras=${hydra1.hp.toInt},${hydra2.hp.toInt} loaded=${reaver.transport.isDefined}")
    assert(escort.delivered, "ferry never reached the drop")
    assert( ! sunken.alive, "dropped reaver never cleared the sunken")
    assert( ! drone1.alive && ! drone2.alive, "dropped reaver never cleared the drones")
    assert(drop.boards > 0, "pressured hull never reboarded")
    assert(reaver.transport.isDefined, "hull is not aboard for extraction")
    assert(reaver.alive && shuttle.alive, "pair died flying the cycle")
  }
}
