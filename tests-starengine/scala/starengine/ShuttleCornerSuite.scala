package starengine

import Micro.Actions.Combat.Maneuvering.AirFlee
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Shuttle corner escape: three Hydralisks press a Shuttle sitting two
 * tiles right and eight down from the top-left corner, and the only way
 * out is up - straight into the corner trap. The pilot flees the
 * centroid on the shipped avoidance rule, so it slides up along the
 * corner square instead of diving in, and never comes within four tiles
 * of the corner. Focused fire can still kill it - this proves the
 * escape line, not invulnerability. Speeds, reaches and damage are
 * the real UnitType numbers; the map is 1024px square.
 */
class ShuttleCornerSuite extends AnyFunSuite {

  private val mapPx = 1024

  /** Flee the centroid on the shipped rule, corner-guarded. */
  private class CornerFlee extends Pilot {
    var lastDest: Option[(Double, Double)] = None

    override def direct(me: Body, world: World): Directive = {
      val threats = world.foes.filter(f => f.alive && f.kind.canAttack(me.kind))
      AirFlee.fleePixel(
        Mathematics.Points.Pixel(me.x.toInt, me.y.toInt),
        threats.map(f => Mathematics.Points.Pixel(f.x.toInt, f.y.toInt)),
        me.lossFraction,
        mapPx,
        mapPx).map(dest => {
        lastDest = Some((dest.x.toDouble, dest.y.toDouble))
        MoveTo(dest.x, dest.y)
      }).getOrElse(Hold)
    }
  }

  test("a pressed shuttle flees up without entering the corner") {
    val shuttle = new Body(Protoss.Shuttle, 2 * 32.0, 8 * 32.0, Protoss.Shuttle.maxTotalHealth)
    val hydras = Seq(
      new Body(Zerg.Hydralisk, 2 * 32.0, 10 * 32.0, Zerg.Hydralisk.maxTotalHealth),
      new Body(Zerg.Hydralisk, 3 * 32.0, 10 * 32.0, Zerg.Hydralisk.maxTotalHealth),
      new Body(Zerg.Hydralisk, 4 * 32.0, 10 * 32.0, Zerg.Hydralisk.maxTotalHealth))
    val flee = new CornerFlee
    val pilots: Map[Body, Pilot] = Map(shuttle -> flee) ++
      hydras.map(_ -> new Pilots.ZombieAttack(_.ours, (2 * 32.0, 10 * 32.0))).toMap
    val world = new World(Seq(shuttle), hydras, pilots, Seq.empty)

    println(s"DIAG shuttle speed=${shuttle.speed} hydra speed=${hydras.head.speed} hydra range=${hydras.head.rangeAgainst(shuttle)}")
    var minCorner = Double.PositiveInfinity
    var cornerDest = false
    while (world.frame < 1500 && shuttle.alive) {
      world.step()
      minCorner = Math.min(minCorner, Math.hypot(shuttle.x, shuttle.y))
      flee.lastDest.foreach(d => if (d._1 < 6 * 32 && d._2 < 6 * 32) cornerDest = true)
    }

    println(s"DIAG end=(${shuttle.x.toInt},${shuttle.y.toInt}) minCorner=${minCorner.toInt}px alive=${shuttle.alive} frames=${world.frame} cornerDest=$cornerDest")
    assert(minCorner >= 4 * 32.0, f"shuttle entered the corner: closest ${minCorner.toInt}px")
    assert( ! cornerDest, "a flee destination landed in the unusable 6x6 corner square")
    assert(shuttle.y < 8 * 32.0 - 32.0, "shuttle did not flee upward")
  }
}
