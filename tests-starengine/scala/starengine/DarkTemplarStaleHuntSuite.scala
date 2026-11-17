package starengine

import Micro.Actions.Protoss.DarkTemplar.DarkTemplarPursuit
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Stale-hunt give-up: a remembered contact is a fix that ages. The live
 * DT_v_Protoss game parked its Templar on a ghost pixel for 300 frames -
 * DarkTemplarHold every frame, gap frozen, the dragoon fighting elsewhere
 * - because a sub-three-minute memory re-arms the hunt on arrival and the
 * ghost counts as a victim, so neither the hunt nor the cohesion leg lets
 * go. The fixed brain drops a ghost it has already closed on without
 * re-seeing (arrival-gated, never time-gated: long walks keep walking)
 * and walks back to a fighting squad past the shared fourteen-tile band.
 * Speeds, reaches and sights are the real UnitType numbers; both pilots
 * call the shipped kernels, the engine owns the loop only.
 *
 * One duel, two brains. A Mutalisk shows itself once, then outruns the
 * Templar north for good; the squad duels on at the anchor to the south.
 * The old brain holds the ghost pixel to the end of the run, alone. The
 * fixed brain drops the ghost on arrival and rejoins. Knowledge is
 * modeled, not omniscient: seen means inside our own sight, memory holds
 * the last-seen fix, squadFighting is scenario-fixed (mirrors a
 * squadmate in battle, which the engine does not model).
 */
class DarkTemplarStaleHuntSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.DT.name + StarEngineTags.Hunt.name) }

  private val anchorX = 0.0
  private val anchorY = -800.0

  private class GhostFlight(goal: (Double, Double)) extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      if (Math.hypot(me.x - goal._1, me.y - goal._2) <= 32.0) Hold
      else MoveTo(goal._1, goal._2)
    }
  }

  private class DTHunter(
    mate: Body,
    ghost: Body,
    useGiveUp: Boolean,
    squadFighting: Boolean) extends Pilot {
    var memory = Map.empty[Body, (Double, Double, Int)]
    var dropFrame: Option[Int] = None
    var rejoinFrame: Option[Int] = None
    var minDistToAnchor: Double = Double.PositiveInfinity

    override def direct(me: Body, world: World): Directive = {
      val dtSight = me.kind.sightPixels.toDouble
      minDistToAnchor = Math.min(minDistToAnchor, Math.hypot(me.x - anchorX, me.y - anchorY))

      // Memory: last-seen fixes of live foes, forgotten only by the kernel.
      world.foes.filter(f => f.alive && me.dist(f) <= dtSight).foreach(f => memory += f -> (f.x, f.y, world.frame))

      val guns = world.foes.filter(f => f.alive && f.kind != Protoss.Observer && me.dist(f) <= dtSight)
      val nearGuns = guns.filter(g => me.dist(g) / 32.0 <= 13.0)
      if (nearGuns.nonEmpty) {
        val target = nearGuns.minBy(me.dist)
        val (dx, dy) = away(me, target.x, target.y)
        return MoveTo(me.x + dx * 128.0, me.y + dy * 128.0)
      }

      // Hunt: the nearest seen gun, else the freshest remembered fix the
      // kernel keeps, else the squad anchor when the squad fights on.
      val seen = guns.sortBy(me.dist).headOption
      val remembered = memory.toSeq
        .filter { case (foe, _) => foe.alive && me.dist(foe) > dtSight }
        .sortBy { case (_, (_, _, frame)) => -frame }
        .flatMap { case (foe, (fx, fy, _)) =>
          val tiles = Math.hypot(me.x - fx, me.y - fy) / 32.0
          if (useGiveUp && DarkTemplarPursuit.shouldDropGhost(tiles, ghostVisible = false)) {
            // Pilot-owned memory, so the drop deletes the fix outright
            // (mirrors the production latch: a re-sighting re-adds it,
            // which is the release arm over there).
            memory -= foe
            if (dropFrame.isEmpty && foe == ghost) dropFrame = Some(world.frame)
            None
          } else Some((fx, fy))
        }.headOption
      seen match {
        case Some(prey) => Chase(prey)
        case None => remembered match {
          case Some((gx, gy)) => MoveTo(gx, gy)
          case None =>
            val contact = world.foes.exists(f => f.alive && me.dist(f) <= me.rangeAgainst(f) + 64.0)
            if (useGiveUp && mate.alive && DarkTemplarPursuit.shouldRejoinSquad(
              Math.hypot(me.x - mate.x, me.y - mate.y) / 32.0, squadFighting, contact)) {
              if (rejoinFrame.isEmpty) rejoinFrame = Some(world.frame)
              MoveTo(mate.x, mate.y)
            } else Hold
        }
      }
    }

    private def away(me: Body, x: Double, y: Double): (Double, Double) = {
      val dx = me.x - x
      val dy = me.y - y
      val dist = Math.sqrt(dx * dx + dy * dy)
      if (dist > 0) (dx / dist, dy / dist) else (0.0, 1.0)
    }
  }

  private case class Outcome(
    endX: Double, endY: Double,
    dropFrame: Option[Int], rejoinFrame: Option[Int], minDistToAnchor: Double)

  private def runHunt(useGiveUp: Boolean): Outcome = {
    val dt = new Body(Protoss.DarkTemplar, 0.0, -300.0, Protoss.DarkTemplar.maxTotalHealth)
    dt.cloaked = true
    // Frozen tableau: the mate holds the anchor, a hostile gun holds far
    // south out of the way. The fight itself is scenario-fixed
    // (squadFighting) - what the suite pins is the wiring from the drop
    // through the rejoin to the walk home, which a live duel's gravity
    // well would only obscure.
    val mate = new Body(Protoss.Dragoon, anchorX, anchorY, Protoss.Dragoon.maxTotalHealth)
    val distant = new Body(Protoss.Dragoon, 0.0, -1100.0, Protoss.Dragoon.maxTotalHealth)
    val ghost = new Body(Zerg.Mutalisk, 0.0, -100.0, Zerg.Mutalisk.maxTotalHealth)
    val pilot = new DTHunter(mate, ghost, useGiveUp, squadFighting = true)
    val pilots: Map[Body, Pilot] = Map(
      dt -> pilot,
      mate -> new Pilots.Hold,
      distant -> new Pilots.Hold,
      ghost -> new GhostFlight((0.0, 700.0)))
    val world = new World(Seq(dt, mate), Seq(distant, ghost), pilots, Seq.empty)
    var f = 0
    while (f < 600 && dt.alive) {
      world.step()
      if (f % 100 == 0) println(s"DIAG t=$f dt=(${dt.x.toInt},${dt.y.toInt}) ghost=(${ghost.x.toInt},${ghost.y.toInt},alive=${ghost.alive}) mate=(${mate.x.toInt},${mate.y.toInt},alive=${mate.alive})")
      f += 1
    }
    Outcome(dt.x, dt.y, pilot.dropFrame, pilot.rejoinFrame, pilot.minDistToAnchor)
  }

  test("a hunter without the give-up never comes home") {
    val old = runHunt(useGiveUp = false)
    println(s"DIAG old end=(${old.endX.toInt},${old.endY.toInt}) minAnchor=${old.minDistToAnchor.toInt}px")
    assert(old.dropFrame.isEmpty, "no kernel, no drop - the control must hold the ghost")
    assert(old.rejoinFrame.isEmpty, "no kernel, no rejoin - the control must hold the ghost")
    assert(old.minDistToAnchor > 400.0, "the control came home on its own - nothing to fix here")
  }

  test("a hunter with the give-up drops the ghost and rejoins the fight") {
    val dtSight = Protoss.DarkTemplar.sightPixels.toDouble
    println(s"DIAG dtSight=$dtSight dtSpeed=${Protoss.DarkTemplar.topSpeed} mutaSpeed=${Zerg.Mutalisk.topSpeed}")
    val old = runHunt(useGiveUp = false)
    val fixed = runHunt(useGiveUp = true)
    println(s"DIAG fixed end=(${fixed.endX.toInt},${fixed.endY.toInt}) drop@${fixed.dropFrame} rejoin@${fixed.rejoinFrame} minAnchor=${fixed.minDistToAnchor.toInt}px")
    assert(fixed.dropFrame.isDefined, "the ghost was never dropped - the hunt still feeds the memory")
    assert(fixed.rejoinFrame.isDefined, "the squad fought on without its hunter - the rejoin never fired")
    assert(fixed.rejoinFrame.exists(_ >= fixed.dropFrame.getOrElse(Int.MaxValue)), "the rejoin must not precede the drop")
    assert(fixed.minDistToAnchor <= 14 * 32.0 + 32.0, "the hunter never made it back to the squad's band")
    assert(fixed.minDistToAnchor < old.minDistToAnchor, "the hunter came home no closer than the control")
  }
}
