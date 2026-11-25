package starengine

import Mathematics.Maff
import Mathematics.Points.Pixel
import Micro.Actions.Combat.Maneuvering.{AirFlee, CornerAntiStuckGuard}
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Corsair corner escape: a Hydralisk presses a Corsair sitting inside the
 * 20-tile corner zone, and the only way out bends off the corner - fleeing
 * straight off the Hydralisk hugs the trap until the guns close in (the
 * live death). The pilot flees the shipped rule verbatim (worst threat
 * outside the zone, centroid-plus-corner blend inside it, guard on the
 * Corsair width), so the escape leaves the zone instead of milling in it.
 * Survival is the escape's proof here: a Corsair outruns a Hydralisk, so
 * dying inside the zone means the direction was wrong, not the legs.
 */
class CorsairCornerSuite extends AnyFunSuite with StarEngineSuite {

  private val mapPx = 2048

  /** Shipped CorsairRangedFlee math, verbatim: worst threat scored, centroid-plus-corner blend inside the zone. */
  private class CornerFlee extends Pilot {
    var lastDest: Option[(Double, Double)] = None

    override def direct(me: Body, world: World): Directive = {
      val own = Pixel(me.x.toInt, me.y.toInt)
      val threats = world.foes.filter(f => f.alive && f.kind.canAttack(me.kind)).map(f => Pixel(f.x.toInt, f.y.toInt))
      val inTrigger = threats.filter(t => own.pixelDistance(t) < 14.0 * 32.0)
      val marginPx = AirFlee.marginTiles(me.lossFraction, AirFlee.marginLowTiles, AirFlee.marginHighTiles) * 32.0
      val away =
        if (inTrigger.nonEmpty && CornerAntiStuckGuard.inCornerZone(own, mapPx, mapPx, CornerAntiStuckGuard.corsairTiles))
          Some(CornerAntiStuckGuard.blendedEscape(own, Maff.centroid(inTrigger), CornerAntiStuckGuard.nearestCorner(own, mapPx, mapPx), marginPx))
        else
          AirFlee.fleeScored(
            own,
            threats.map(t => (t, 14.0 * 32.0 - own.pixelDistance(t))),
            _ > 0.0,
            marginPx,
            mapPx,
            mapPx,
            CornerAntiStuckGuard.corsairTiles)
      away.map(dest0 => {
        val dest = CornerAntiStuckGuard.guard(dest0, mapPx, mapPx, CornerAntiStuckGuard.corsairTiles)
        lastDest = Some((dest.x.toDouble, dest.y.toDouble))
        MoveTo(dest.x, dest.y)
      }).getOrElse(Hold)
    }
  }

  test("a pressed corsair bends off the corner and leaves the 20-tile zone", StarEngineTags.Corsair) {
    val corsair = new Body(Protoss.Corsair, 500.0, 120.0, Protoss.Corsair.maxTotalHealth)
    val hydra = new Body(Zerg.Hydralisk, 500.0, 420.0, Zerg.Hydralisk.maxTotalHealth)
    println(s"DIAG corsair speed=${corsair.speed} hydra speed=${hydra.speed} hydra range=${hydra.rangeAgainst(corsair)}")
    val flee = new CornerFlee
    val pilots: Map[Body, Pilot] = Map(
      corsair -> flee,
      hydra -> new Pilots.ZombieAttack(_.ours, (500.0, 120.0)))
    val world = new World(Seq(corsair), Seq(hydra), pilots, Seq.empty)
    var minCorner = Double.PositiveInfinity
    var cornerDest = false
    while (world.frame < 1500 && corsair.alive) {
      world.step()
      minCorner = Math.min(minCorner, Math.hypot(corsair.x, corsair.y))
      flee.lastDest.foreach(d => if (d._1 < 20 * 32 && d._2 < 20 * 32) cornerDest = true)
    }
    val finalCorner = Math.hypot(corsair.x, corsair.y)
    println(s"DIAG end=(${corsair.x.toInt},${corsair.y.toInt}) minCorner=${minCorner.toInt}px finalCorner=${finalCorner.toInt}px alive=${corsair.alive} frames=${world.frame} cornerDest=$cornerDest")
    assert(corsair.alive, "the corsair outruns the hydra - dying means the escape aimed wrong")
    assert(finalCorner > 20 * 32, f"the escape never left the corner zone: ends ${finalCorner.toInt}px from the corner")
    assert( ! cornerDest, "a flee destination landed inside the unusable 20x20 corner square")
  }
}
