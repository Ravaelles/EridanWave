package starengine

import Micro.Actions.Combat.Spells.Protoss.PsionicStorm
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * High Templars storm Hydralisk packs (StarEngine repro of a live game):
 * six Templars on a full battery watched Hydralisks walk in and never
 * cast. The pilot below calls the shipped storm kernels - friendly veto,
 * thin-pack ladder, Overlord veto - so a cast proves the shipped rule
 * permits it, not a copy. Storm numbers (cost, damage, box) live in the
 * World companion, read by both the engine and the pilot.
 */
class StormScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Psionic.name + StarEngineTags.Templar.name) }

  /** Templar that storms the densest in-reach pack the shipped vetoes allow, else holds. */
  private class TemplarStorm extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val stormable = world.foes.filter(f => f.alive && f.kind.canBeStormed)
      if (me.energy >= World.StormCostEnergy && stormable.nonEmpty) {
        val inReach = stormable.filter(f => me.dist(f) <= World.StormCastRangePx + World.StormBoxHalfPx)
        var best: Option[Body] = None
        var bestFoes = 0
        var bestFriends = 0
        inReach.foreach(center => {
          val foes = stormable.count(f =>
            Math.abs(f.x - center.x) <= World.StormBoxHalfPx && Math.abs(f.y - center.y) <= World.StormBoxHalfPx)
          val friends = world.ours.count(o =>
            o.alive && (Math.abs(o.x - center.x) <= World.StormBoxHalfPx && Math.abs(o.y - center.y) <= World.StormBoxHalfPx))
          if (foes > bestFoes || (foes == bestFoes && best.exists(b => me.dist(center) < me.dist(b)))) {
            best = Some(center)
            bestFoes = foes
            bestFriends = friends
          }
        })
        // No Reavers or sieged Tanks in these scenarios, so the crucial
        // bypass never applies; the pack ladder decides alone.
        best.foreach(center => {
          if ( ! PsionicStorm.vetoFriendlyHits(bestFriends)
            && ! PsionicStorm.vetoThinPack(bestFoes, hasCrucialTarget = false, me.energy, enemyIsProtoss = false)
            && ! PsionicStorm.vetoOverlord(center.kind == Zerg.Overlord, me.energy)) {
            return CastStorm(center.x, center.y)
          }
        })
      }

      Hold
    }
  }

  private def hydraPack(n: Int, cx: Double, cy: Double): Seq[Body] =
    (0 until n).map(i => {
      val b = new Body(Zerg.Hydralisk, cx + (i % 4) * 40.0 - 60.0, cy + (i / 4) * 40.0, Zerg.Hydralisk.maxTotalHealth)
      b
    })

  private def lineTemplar(x: Double, y: Double, energy: Int): Body = {
    val b = new Body(Protoss.HighTemplar, x, y, Protoss.HighTemplar.maxTotalHealth)
    b.energy = energy
    b
  }

  test("a full battery storms the hydra pack instead of watching") {
    val zealots = Seq(
      new Body(Protoss.Zealot, -40.0, 500.0, Protoss.Zealot.maxTotalHealth),
      new Body(Protoss.Zealot, 40.0, 500.0, Protoss.Zealot.maxTotalHealth))
    val dragoons = Seq(
      new Body(Protoss.Dragoon, -120.0, 520.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 120.0, 520.0, Protoss.Dragoon.maxTotalHealth))
    val templars = (-250 to 250 by 100).map(x => lineTemplar(x.toDouble, 430.0, 200)).toSeq
    val hydras = hydraPack(8, 0.0, 250.0)
    val ours = zealots ++ dragoons ++ templars
    val pilots: Map[Body, Pilot] = (
      zealots.map(_ -> new Pilots.AttackMove(_.foes, (me, f) => me.canHit(f), (0.0, 500.0))).toMap
        ++ dragoons.map(_ -> new Pilots.AttackMove(_.foes, (me, f) => me.canHit(f), (0.0, 520.0))).toMap
        ++ templars.map(_ -> new TemplarStorm).toMap
        ++ hydras.map(h => h -> new Pilots.ZombieAttack(_.ours, (0.0, 500.0))).toMap).toMap
    val world = new World(ours, hydras, pilots, Seq.empty)

    var hydrasDeadFrame: Option[Int] = None
    while (world.frame < 600 && hydras.exists(_.alive) && ours.exists(_.alive)) {
      world.step()
      if (hydrasDeadFrame.isEmpty && hydras.forall(! _.alive)) hydrasDeadFrame = Some(world.frame)
    }

    val storms = templars.map(t => world.stormsCast.getOrElse(t, 0)).sum
    println(s"DIAG storms=$storms hydrasDead=$hydrasDeadFrame templarsAlive=${templars.count(_.alive)}/6")
    assert(storms >= 4, "full battery never stormed the pack")
    assert(hydrasDeadFrame.isDefined, "hydra pack survived six templars")
    assert(templars.forall(_.alive), "templars died storming from behind their line")
  }

  test("a dry battery holds fire even with hydras in reach") {
    val zealots = Seq(
      new Body(Protoss.Zealot, -40.0, 500.0, Protoss.Zealot.maxTotalHealth),
      new Body(Protoss.Zealot, 40.0, 500.0, Protoss.Zealot.maxTotalHealth))
    val templars = Seq(lineTemplar(-50.0, 430.0, 50), lineTemplar(50.0, 430.0, 50))
    val hydras = hydraPack(4, 0.0, 250.0)
    val ours = zealots ++ templars
    val pilots: Map[Body, Pilot] = (
      zealots.map(_ -> new Pilots.AttackMove(_.foes, (me, f) => me.canHit(f), (0.0, 500.0))).toMap
        ++ templars.map(_ -> new TemplarStorm).toMap
        ++ hydras.map(h => h -> new Pilots.ZombieAttack(_.ours, (0.0, 500.0))).toMap).toMap
    val world = new World(ours, hydras, pilots, Seq.empty)

    while (world.frame < 600 && hydras.exists(_.alive) && zealots.exists(_.alive)) {
      world.step()
    }

    val storms = templars.map(t => world.stormsCast.getOrElse(t, 0)).sum
    println(s"DIAG dry storms=$storms")
    assert(storms == 0, "dry battery stormed below the 75-energy floor")
  }
}
