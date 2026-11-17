package starengine

import Mathematics.Points.Pixel
import Micro.Actions.Combat.Maneuvering.{AirFlee, CornerAntiStuckGuard}
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Scored air-flee proof (production order, mirrored call for call): the
 * Corsair scores plain-distance penetration against its own 14-tile
 * trigger with a sharp edge and runs from the worst gun, not the middle;
 * the Observer does the same against its 12-tile trigger with the eye's
 * 6-to-12 margin. Both must stay alive and killing in a live duel - the
 * worst-focus leg and the sharp edge carry real fights, not just pixels.
 */
class ScoredFleeSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Retreat.name + StarEngineTags.Corsair.name + StarEngineTags.Observer.name) }

  /** Mirrors CorsairRangedFlee: flee scored threats first, hunt second. */
  private class CorsairScored extends Pilot {
    var flees = 0

    override def direct(me: Body, world: World): Directive = {
      val here = Pixel(me.x.toInt, me.y.toInt)
      val ranged = world.foes.view.filter(f => f.alive && f.kind.canAttack(me.kind)).map(f => Pixel(f.x.toInt, f.y.toInt)).toSeq
      AirFlee.fleeScored(
        here,
        ranged.map(t => (t, 14.0 * 32.0 - here.pixelDistance(t))),
        _ > 0.0,
        AirFlee.marginTiles(me.lossFraction, AirFlee.marginLowTiles, AirFlee.marginHighTiles) * 32.0,
        8192, 8192,
        CornerAntiStuckGuard.corsairTiles).foreach(dest => {
        flees += 1

        return MoveTo(dest.x, dest.y)
      })
      world.nearestFoeOf(me, world.foes, (_, _) => true) match {
        case Some(foe) => Chase(foe)
        case None      => Hold
      }
    }
  }

  test("scored corsairs keep out of hydra reach while the overlords die") {
    val corsairs = Seq(
      new Body(Protoss.Corsair, 0.0, 500.0, Protoss.Corsair.maxTotalHealth),
      new Body(Protoss.Corsair, 80.0, 500.0, Protoss.Corsair.maxTotalHealth))
    val overlords = Seq(
      new Body(Zerg.Overlord, 0.0, 100.0, Zerg.Overlord.maxTotalHealth),
      new Body(Zerg.Overlord, 120.0, 80.0, Zerg.Overlord.maxTotalHealth))
    // Guns off the firing lane: the approach crosses the 14-tile trigger
    // (flee fires), but the line the kills need stands clear of it, so the
    // wing re-engages instead of milling at the edge.
    val hydras = Seq(
      new Body(Zerg.Hydralisk, 500.0, 150.0, Zerg.Hydralisk.maxTotalHealth),
      new Body(Zerg.Hydralisk, 560.0, 150.0, Zerg.Hydralisk.maxTotalHealth),
      new Body(Zerg.Hydralisk, 620.0, 150.0, Zerg.Hydralisk.maxTotalHealth))
    val hunts = corsairs.map(_ -> new CorsairScored).toMap
    val foes = overlords ++ hydras
    val world = new World(corsairs, foes,
      hunts ++ foes.map(_ -> new Pilots.Hold).toMap, Seq.empty)

    val hydraRange = hydras.head.rangeAgainst(corsairs.head)
    var minDist = Double.PositiveInfinity
    while (world.frame < 4000 && overlords.exists(_.alive) && corsairs.exists(_.alive)) {
      world.step()
      corsairs.foreach(c => hydras.foreach(h =>
        if (h.alive) minDist = Math.min(minDist, c.dist(h))))
    }

    val fled = hunts.values.map(_.flees).sum
    println(s"DIAG scored hydra range=$hydraRange minDist=${minDist.toInt}px overlordsLeft=${overlords.count(_.alive)} fled=$fled")
    assert(overlords.forall(! _.alive), "covered overlords survived")
    assert(corsairs.forall(_.alive), "hydras caught a corsair")
    assert(minDist > hydraRange, "corsairs flew inside hydra reach")
    assert(fled > 0, "scored flee never fired with real guns around")
  }

  /** Mirrors BeDetector.fleeAttackers: scored guns first, hold otherwise. */
  private class ScoredEye extends Pilot {
    var flees = 0

    override def direct(me: Body, world: World): Directive = {
      val here = Pixel(me.x.toInt, me.y.toInt)
      val guns = world.foes.view.filter(f => f.alive && f.kind.canAttack(me.kind)).map(f => Pixel(f.x.toInt, f.y.toInt)).toSeq
      AirFlee.fleeScored(
        here,
        guns.map(t => (t, 12.0 * 32.0 - here.pixelDistance(t))),
        _ > 0.0,
        AirFlee.marginTiles(me.lossFraction, 6.0, 12.0) * 32.0,
        8192, 8192).foreach(dest => {
        flees += 1

        return MoveTo(dest.x, dest.y)
      })
      Hold
    }
  }

  private def runEye(pilot: Pilot): (Int, Int) = {
    val eye = new Body(Protoss.Observer, 0.0, 500.0, Protoss.Observer.maxTotalHealth)
    val muta = new Body(Zerg.Mutalisk, 400.0, 150.0, Zerg.Mutalisk.maxTotalHealth)
    val world = new World(Seq(eye), Seq(muta),
      Map(eye -> pilot, muta -> new Pilots.ZombieAttack(_.ours, (400.0, 150.0))), Seq.empty)
    while (world.frame < 600 && eye.alive) {
      world.step()
    }

    (world.frame, pilot.asInstanceOf[ScoredEye].flees)
  }

  test("a scored eye outlives a parked eye against a pressing muta") {
    val (scoredFrames, fled) = runEye(new ScoredEye)
    val parkedFrames = {
      val eye = new Body(Protoss.Observer, 0.0, 500.0, Protoss.Observer.maxTotalHealth)
      val muta = new Body(Zerg.Mutalisk, 400.0, 150.0, Zerg.Mutalisk.maxTotalHealth)
      val world = new World(Seq(eye), Seq(muta),
        Map(eye -> new Pilots.Hold, muta -> new Pilots.ZombieAttack(_.ours, (400.0, 150.0))), Seq.empty)
      while (world.frame < 600 && eye.alive) {
        world.step()
      }

      world.frame
    }
    println(s"DIAG scored alive=${scoredFrames}f fled=$fled parked alive=${parkedFrames}f")
    assert(fled > 0, "scored flee never fired with a gun pressing")
    assert(scoredFrames > parkedFrames, "scored flee did not buy the eye any time")
  }
}
