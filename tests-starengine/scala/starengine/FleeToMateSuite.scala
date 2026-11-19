package starengine

import Micro.Actions.Protoss.ReaverShuttle.PairArbiter
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Glued retreat, step by step: a Shuttle with its unloaded Reaver on foot
 * takes fire. The pilot calls the shipped resolveShuttle on the same
 * plain views the game seam builds and executes the order the way the
 * brains do: inside the glue band the ride retreats TO the mate (pickup
 * on arrival, then both fly), never away from it. Past the band the bond
 * is broken and the ride saves itself alone. Speeds, reaches and damage
 * are the real UnitType numbers.
 */
class FleeToMateSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Shuttle.name + StarEngineTags.Rejoin.name) }

  private def tilePos(b: Body): (Double, Double) = (b.x / 32.0, b.y / 32.0)

  private def threatViews(me: Body, foes: Seq[Body]): Seq[PairArbiter.ThreatView] =
    foes.filter(f => f.alive && f.canHit(me)).map(f =>
      PairArbiter.ThreatView(me.dist(f) / 32.0, f.rangeAgainst(me) / 32.0))

  private def mateView(mate: Body): PairArbiter.ReaverView =
    PairArbiter.ReaverView(
      tilePos(mate),
      Some(1),
      loaded = false,
      None,
      1,
      activeScarab = false,
      0,
      wound01 = 0.0,
      unloadOrdered = false,
      None,
      attackedRecently = false,
      boardLocked = false,
      Double.PositiveInfinity,
      longTrip = false,
      stalledShot = false,
      Seq.empty)

  /** Pilot, production order: the arbiter decides, legs execute. */
  private class GluePilot extends Pilot {
    var toMate = 0
    var away = 0

    override def direct(me: Body, world: World): Directive = {
      val mates = world.ours.view.filter(o => o.alive && o.kind == Protoss.Reaver).map(mateView).toSeq
      val out = PairArbiter.resolveShuttle(
        PairArbiter.ShuttleView(tilePos(me), Some(1), hasReaverAboard = false, threatViews(me, world.foes)),
        mates,
        raid = false,
        baitRide = false).shuttle
      val mate = world.ours.find(o => o.alive && o.kind == Protoss.Reaver)
      out match {
        case PairArbiter.FleeToMate =>
          toMate += 1
          mate.map(m => MoveTo(m.x, m.y)).getOrElse(Hold)
        case PairArbiter.Flee =>
          away += 1
          world.foes.filter(_.alive).sortBy(me.dist).headOption match {
            case Some(t) =>
              val dx = me.x - t.x
              val dy = me.y - t.y
              val dist = Math.sqrt(dx * dx + dy * dy)
              if (dist > 0) MoveTo(me.x + dx / dist * 256.0, me.y + dy / dist * 256.0)
              else Hold
            case None => Hold
          }
        case _ => mate.map(m => MoveTo(m.x, m.y)).getOrElse(Hold)
      }
    }
  }

  test("a pressured ride retreats to its mate inside the band, never away") {
    val shuttle = new Body(Protoss.Shuttle, 0.0, 500.0, Protoss.Shuttle.maxTotalHealth)
    val reaver = new Body(Protoss.Reaver, 160.0, 500.0, Protoss.Reaver.maxTotalHealth)
    val marine = new Body(Terran.Marine, 300.0, 500.0, Terran.Marine.maxTotalHealth)
    val pilot = new GluePilot
    val world = new World(Seq(shuttle, reaver), Seq(marine),
      Map(shuttle -> pilot, reaver -> new Pilots.Hold, marine -> new Pilots.Hold), Seq.empty)

    var f = 0
    var startDist = -1.0
    while (f < 600 && shuttle.alive) {
      if (f == 0) startDist = shuttle.dist(reaver)
      world.step()
      f = world.frame
    }
    println(s"DIAG glue toMate=${pilot.toMate} away=${pilot.away} start=$startDist end=${shuttle.dist(reaver)}")
    assert(pilot.away == 0, "ride flew away from its mate under fire")
    assert(pilot.toMate > 0, "ride never took the glued retreat")
    assert(shuttle.dist(reaver) < startDist, "ride never closed with its mate")
  }

  test("past the band the bond is broken and the ride saves itself") {
    val shuttle = new Body(Protoss.Shuttle, 0.0, 500.0, Protoss.Shuttle.maxTotalHealth)
    val reaver = new Body(Protoss.Reaver, 960.0, 500.0, Protoss.Reaver.maxTotalHealth)
    val marine = new Body(Terran.Marine, 192.0, 500.0, Terran.Marine.maxTotalHealth)
    val pilot = new GluePilot
    val world = new World(Seq(shuttle, reaver), Seq(marine),
      Map(shuttle -> pilot, reaver -> new Pilots.Hold, marine -> new Pilots.Hold), Seq.empty)

    var f = 0
    var startThreat = -1.0
    while (f < 600 && shuttle.alive) {
      if (f == 0) startThreat = shuttle.dist(marine)
      world.step()
      f = world.frame
    }
    println(s"DIAG alone toMate=${pilot.toMate} away=${pilot.away} threat=$startThreat end=${shuttle.dist(marine)}")
    assert(pilot.toMate == 0, "ride chased a mate past the band")
    assert(pilot.away > 0, "pressured ride never fled")
    assert(shuttle.dist(marine) > startThreat, "ride never opened the distance")
  }
}
