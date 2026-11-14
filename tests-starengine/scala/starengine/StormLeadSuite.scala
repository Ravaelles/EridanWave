package starengine

import Micro.Actions.Combat.Spells.Protoss.PsionicStorm
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Storm lead with real numbers (HT speed 3.2 px/f vs Hydralisk 3.66,
 * cast from 11 tiles): four Hydralisks drift down-left past a lone
 * High Templar standing to their left, and the Templar storms the
 * point they will reach when the cast lands - StormCastFrames later,
 * the shipped iScript-derived cast time, pinned equal to it by suite.
 * The pilot derives the pack velocity from observed positions (never
 * hardcoded speeds) and calls the shipped veto kernels, so a kill
 * proves the shipped rule permits the cast and the lead math holds.
 */
class StormLeadSuite extends AnyFunSuite with StarEngineSuite {

  /** Hydralisk drift: down-left, full speed, every frame. */
  private class DownLeft extends Pilot {
    override def direct(me: Body, world: World): Directive =
      MoveTo(me.x - 200.0, me.y + 200.0)
  }

  /** Templar that storms the led pack centroid, approaches out of range, holds after one cast. */
  private class LeadStorm extends Pilot {
    private var prev: Option[(Double, Double)] = None
    var castFrame: Option[Int] = None
    var castPoint: Option[(Double, Double)] = None
    var castCentroid: Option[(Double, Double)] = None
    var landingCentroid: Option[(Double, Double)] = None

    private def centroid(live: Seq[Body]): (Double, Double) =
      (live.map(_.x).sum / live.size, live.map(_.y).sum / live.size)

    override def direct(me: Body, world: World): Directive = {
      val live = world.foes.filter(f => f.alive && f.kind.canBeStormed)
      if (live.isEmpty) return Hold

      val now = centroid(live)
      val vel = prev.map(p => (now._1 - p._1, now._2 - p._2)).getOrElse((0.0, 0.0))
      prev = Some(now)

      if (castFrame.isDefined && world.frame == castFrame.get + World.StormCastFrames) {
        landingCentroid = Some(now)
      }

      if (world.stormsCast.getOrElse(me, 0) > 0) return Hold

      if (me.energy >= World.StormCostEnergy) {
        val lead = (now._1 + vel._1 * World.StormCastFrames, now._2 + vel._2 * World.StormCastFrames)
        val inBox = live.filter(f =>
          Math.abs(f.x - lead._1) <= World.StormBoxHalfPx && Math.abs(f.y - lead._2) <= World.StormBoxHalfPx)
        val nearest = if (inBox.isEmpty) None else Some(inBox.minBy(f => Math.hypot(f.x - lead._1, f.y - lead._2)))
        if (Math.hypot(me.x - lead._1, me.y - lead._2) <= World.StormCastRangePx
          && ! PsionicStorm.vetoFriendlyHits(0)
          && ! PsionicStorm.vetoThinPack(inBox.size, hasCrucialTarget = false, me.energy, enemyIsProtoss = false)
          && ! PsionicStorm.vetoOverlord(nearest.exists(_.kind == Zerg.Overlord), me.energy)) {
          castFrame = Some(world.frame)
          castPoint = Some(lead)
          castCentroid = Some(now)

          return CastStorm(lead._1, lead._2)
        }
      }

      world.nearestFoeOf(me, live, (_, _) => true) match {
        case Some(foe) => MoveTo(foe.x, foe.y)
        case None      => Hold
      }
    }
  }

  test("a templar leads drifting hydralisks by the real cast time") {
    assert(World.StormCastFrames == PsionicStorm.castFrames, "engine cast time drifted from the shipped value")

    val hydras = (0 until 4).map(i =>
      new Body(Zerg.Hydralisk, 352.0 + (i % 2) * 30.0, 400.0 + (i / 2) * 30.0, Zerg.Hydralisk.maxTotalHealth))
    val templar = new Body(Protoss.HighTemplar, 0.0, 400.0, Protoss.HighTemplar.maxTotalHealth)
    templar.energy = 200
    val lead = new LeadStorm
    val pilots: Map[Body, Pilot] = Map(templar -> lead) ++ hydras.map(_ -> new DownLeft).toMap
    val world = new World(Seq(templar), hydras, pilots, Seq.empty)

    println(s"DIAG HT speed=${templar.speed} hydra speed=${hydras.head.speed} gap=${hydras.map(templar.dist).min.toInt}px")
    var killFrame: Option[Int] = None
    while (world.frame < 600 && hydras.exists(_.alive) && templar.alive) {
      world.step()
      if (killFrame.isEmpty && hydras.forall(! _.alive)) killFrame = Some(world.frame)
    }

    val gap = (lead.castPoint.get._1 - lead.landingCentroid.get._1) * (lead.castPoint.get._1 - lead.landingCentroid.get._1) +
      (lead.castPoint.get._2 - lead.landingCentroid.get._2) * (lead.castPoint.get._2 - lead.landingCentroid.get._2)
    val missPx = Math.sqrt(gap)
    println(s"DIAG cast at f=${lead.castFrame} point=${lead.castPoint} centroid=${lead.castCentroid} " +
      s"landing centroid=${lead.landingCentroid} miss=${missPx}px kill=$killFrame storms=${world.stormsCast.getOrElse(templar, 0)}")
    assert(lead.castFrame.isDefined, "templar never cast at the drifting pack")
    assert(missPx <= 8.0, f"lead missed the arrival by $missPx%.1fpx")
    assert(hydras.forall(! _.alive), "led storm did not kill the pack")
    assert(templar.alive, "templar died casting from behind")
    assert(world.stormsCast.getOrElse(templar, 0) == 1, "templar should need exactly one led storm")
  }
}
