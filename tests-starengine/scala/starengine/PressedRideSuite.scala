package starengine

import Micro.Actions.Combat.Maneuvering.{CornerAntiStuckGuard, RangedReach}
import Micro.Actions.Protoss.ReaverShuttle.ReaverShuttleFlight
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable

/**
 * Pressed ride: a Reaver grinds at its anchor while its Shuttle holds the
 * rear and flees on the shipped rule - worst penetration across every
 * attacker, latched, corner-guarded on the Shuttle width - when the pack
 * reaches it. Hydralisks press the line, Zerglings flank the ride. Speeds,
 * reaches and cooldowns are the real UnitType numbers; the pilots call
 * the shipped penetration, wound and guard kernels, the engine owns the
 * loop only. The bar from the pair maps: the Shuttle must not get hit
 * at all while the Reaver does the killing, and neither hull may dither.
 */
class PressedRideSuite extends AnyFunSuite with StarEngineSuite {

  private val mapPx = 2400

  /** Reaver discipline: shoot from the anchor, never chase. */
  private class AnchorPilot(anchor: (Double, Double)) extends Pilot {
    var maxAnchorDist: Double = 0.0

    override def direct(me: Body, world: World): Directive = {
      maxAnchorDist = Math.max(maxAnchorDist, Math.hypot(me.x - anchor._1, me.y - anchor._2))
      val mobiles = world.foes.filter(f => f.alive && ! f.kind.isBuilding)
      if (mobiles.isEmpty) return Hold

      val target = mobiles.minBy(me.dist)
      if (me.dist(target) <= me.rangeAgainst(target)) Hold else MoveTo(anchor._1, anchor._2)
    }
  }

  /**
   * Ride discipline: the production flee selection mirrored - a Scourge
   * inside its radius or a ranged reach plus the wound-scaled band makes
   * a positive penetration, the worst one wins, the latch holds the flee
   * until every trigger clears by the exit margin. Safe hulls hold the
   * rear anchor. The latch lives in the pilot (per body, like the other
   * scenario pilots); the shipped Latch pins the same rule by suite.
   */
  private class RidePilot(rear: (Double, Double)) extends Pilot {
    var fleeing: Boolean = false
    var fledOnce: Boolean = false
    var fleeFlips: Int = 0
    var minCalmHydraPx: Double = Double.PositiveInfinity

    override def direct(me: Body, world: World): Directive = {
      val threats = world.foes.filter(f => f.alive && f.kind.canAttack(me.kind))
      val preempt = (ReaverShuttleFlight.baseFleeMarginTiles +
        ReaverShuttleFlight.woundBonusTiles(me.lossFraction)) * 32.0
      val scored = threats.flatMap(t =>
        if (t.kind == Zerg.Scourge) {
          Some((ReaverShuttleFlight.penetrationPixels(
            me.dist(t), ReaverShuttleFlight.scourgeDistTiles * 32.0), t))
        } else {
          val reach = t.rangeAgainst(me)
          if (RangedReach.isRanged(reach)) {
            Some((ReaverShuttleFlight.penetrationPixels(me.dist(t), reach + preempt), t))
          } else None
        })
      val worst = ReaverShuttleFlight.worstPenetration(scored.map(_._1))
      val was = fleeing
      if (worst > 0) fleeing = true
      else if (worst < -ReaverShuttleFlight.fleeExitMarginPixels) fleeing = false
      if (fleeing && ! was) { fledOnce = true; fleeFlips += 1 }
      if (was && ! fleeing) fleeFlips += 1
      if ( ! fleeing) {
        val hydras = threats.filter(_.kind == Zerg.Hydralisk)
        if (hydras.nonEmpty) {
          minCalmHydraPx = Math.min(minCalmHydraPx, hydras.map(me.dist).min)
        }
        return MoveTo(rear._1, rear._2)
      }

      val focus = scored.maxBy(_._1)._2
      val dx = me.x - focus.x
      val dy = me.y - focus.y
      val dist = Math.max(1.0, Math.hypot(dx, dy))
      val rawX = me.x + dx / dist * preempt
      val rawY = me.y + dy / dist * preempt
      val away = CornerAntiStuckGuard.guard(
        Mathematics.Points.Pixel(rawX.toInt, rawY.toInt),
        mapPx, mapPx, CornerAntiStuckGuard.shuttleTiles)
      MoveTo(away.x.toDouble, away.y.toDouble)
    }
  }

  test("the ride keeps out of reach while the reaver grinds the pack") {
    val reaver = new Body(Protoss.Reaver, 0.0, 700.0, Protoss.Reaver.maxTotalHealth)
    val shuttle = new Body(Protoss.Shuttle, 0.0, 1100.0, Protoss.Shuttle.maxTotalHealth)
    val dragoonMid = new Body(Protoss.Dragoon, 0.0, 640.0, Protoss.Dragoon.maxTotalHealth)
    val dragoonRear = new Body(Protoss.Dragoon, 0.0, 1000.0, Protoss.Dragoon.maxTotalHealth)
    val hydras = (0 until 3).map(i =>
      new Body(Zerg.Hydralisk, -100.0 + i * 100.0, 150.0, Zerg.Hydralisk.maxTotalHealth))
    val lingsNorth = (0 until 3).map(i =>
      new Body(Zerg.Zergling, -80.0 + i * 80.0, 230.0, Zerg.Zergling.maxTotalHealth))
    val scourgeSouth = (0 until 2).map(i =>
      new Body(Zerg.Scourge, -40.0 + i * 80.0, 2200.0, Zerg.Scourge.maxTotalHealth))
    val foes: Seq[Body] = hydras ++ lingsNorth ++ scourgeSouth
    val anchorPilot = new AnchorPilot((0.0, 700.0))
    val ridePilot = new RidePilot((0.0, 1100.0))
    val pilots: Map[Body, Pilot] = Map(
      reaver -> anchorPilot,
      shuttle -> ridePilot,
      dragoonMid -> new Pilots.Hold,
      dragoonRear -> new Pilots.Hold) ++
      foes.map(_ -> new Pilots.ZombieAttack(_.ours, (0.0, 700.0))).toMap
    val world = new World(Seq(reaver, shuttle, dragoonMid, dragoonRear), foes, pilots, Seq.empty)

    while (world.frame < 4000 && foes.exists(_.alive) && reaver.alive) {
      world.step()
    }

    val left = foes.count(_.alive)
    println(f"DIAG left=$left reaverHp=${reaver.hp.toInt} shuttleHp=${shuttle.hp.toInt} " +
      f"calmHydra=${ridePilot.minCalmHydraPx.toInt}px maxAnchor=${anchorPilot.maxAnchorDist.toInt}px " +
      f"fleeFlips=${ridePilot.fleeFlips} rideReissues=${world.reissues.getOrElse(shuttle, 0)} frames=${world.frame}")
    assert(left == 0, "pressured pair did not kill everything")
    assert(shuttle.hp >= 0.9 * Protoss.Shuttle.maxTotalHealth, "the ride got hit fleeing")
    assert(ridePilot.fledOnce, "the ride never fled with guns and scourge around")
    assert(ridePilot.fleeFlips <= 6, "the flee latched on and off instead of holding")
    assert(ridePilot.minCalmHydraPx > Zerg.Hydralisk.pixelRangeGround, "hydras walked into an idle ride")
    assert(anchorPilot.maxAnchorDist <= 6 * 32.0, "the reaver left its anchor instead of grinding")
  }
}
