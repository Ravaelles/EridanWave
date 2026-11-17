package starengine

import Micro.Actions.Combat.Decisioning.StormStand
import Micro.Actions.Combat.Spells.Protoss.PsionicStorm
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Storm stand, step by step: a full-battery Templar faces four holding
 * Hydralisks from eleven tiles - past the search cap, inside the walk-in
 * band - while the verdict says retreat. The old rule obeys the verdict
 * and walks home, never casting. The stand pilot calls the shipped
 * StormStand kernel instead: a loaded hull walks into cast range and the
 * cast itself stays first, so the storm lands on the same push. Speeds,
 * reaches, damage and storm numbers are the real UnitType numbers.
 */
class StormStandSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Psionic.name + StarEngineTags.Templar.name + StarEngineTags.Retreat.name) }

  private val home = (-400.0, 500.0)

  /** Old rule: the retreat verdict owns the frame, the battery walks home. */
  private class RetreatPilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val dx = me.x - home._1
      val dy = me.y - home._2
      val dist = Math.sqrt(dx * dx + dy * dy)
      if (dist > 0) MoveTo(me.x + dx / dist * 256.0, me.y + dy / dist * 256.0)
      else Hold
    }
  }

  /**
   * Stand pilot, production order: the shipped stand kernel decides
   * hold-or-retreat, the cast stays first like the Fight chain, and
   * the walk-in only closes to cast range.
   */
  private class StandPilot extends Pilot {
    var storms = 0

    override def direct(me: Body, world: World): Directive = {
      val foes = world.foes.filter(_.alive)
      val nearestTiles = foes.sortBy(me.dist).headOption.map(me.dist(_) / 32.0).getOrElse(Double.PositiveInfinity)
      if (StormStand.shouldHold(
        isTemplar = me.kind == Protoss.HighTemplar,
        enemyIsZerg = true,
        energyReady = me.energy >= World.StormCostEnergy,
        nearestFoeTiles = nearestTiles,
        fightConsensus = false)) {
        val inReach = foes.filter(f => me.dist(f) <= World.StormCastRangePx)
        if (me.energy >= World.StormCostEnergy && inReach.nonEmpty) {
          val center = inReach.minBy(me.dist)
          val friends = world.ours.count(o =>
            o.alive && Math.abs(o.x - center.x) <= World.StormBoxHalfPx && Math.abs(o.y - center.y) <= World.StormBoxHalfPx)
          if ( ! PsionicStorm.vetoFriendlyHits(friends)) {
            storms += 1

            return CastStorm(center.x, center.y)
          }
        }

        foes.sortBy(me.dist).headOption match {
          case Some(f) if me.dist(f) > World.StormCastRangePx => MoveTo(f.x, f.y)
          case _ => Hold
        }
      } else {
        val dx = me.x - home._1
        val dy = me.y - home._2
        val dist = Math.sqrt(dx * dx + dy * dy)
        if (dist > 0) MoveTo(me.x + dx / dist * 256.0, me.y + dy / dist * 256.0)
        else Hold
      }
    }
  }

  private def hydras: Seq[Body] =
    (0 until 4).map(i =>
      new Body(Zerg.Hydralisk, 300.0 + (i % 2) * 40.0, 480.0 + (i / 2) * 40.0, Zerg.Hydralisk.maxTotalHealth))

  private def runDuel(pilot: Pilot): (Body, Seq[Body], Int, Int) = {
    val templar = new Body(Protoss.HighTemplar, 0.0, 500.0, Protoss.HighTemplar.maxTotalHealth)
    templar.energy = 200
    val foes = hydras
    val world = new World(Seq(templar), foes,
      Map(templar -> pilot) ++ foes.map(_ -> new Pilots.Hold).toMap, Seq.empty)
    var storms = 0
    while (world.frame < 2000 && templar.alive && foes.exists(_.alive)) {
      val before = foes.map(_.hp).sum
      world.step()
      if (foes.map(_.hp).sum < before) storms += 1
    }

    (templar, foes, world.frame, storms)
  }

  test("a retreating Templar walks home and never casts") {
    val (templar, foes, frames, storms) = runDuel(new RetreatPilot)
    println(s"DIAG retreat frames=$frames x=${templar.x.toInt} storms=$storms hydras=${foes.count(_.alive)}/4")
    assert(storms == 0, "retreating hull cast on the way out - nothing to fix")
    assert(foes.forall(_.alive), "hydras died without a storm")
  }

  test("a loaded hull stands in and storms through the retreat verdict") {
    val pilot = new StandPilot
    val (templar, foes, frames, _) = runDuel(pilot)
    println(s"DIAG stand frames=$frames x=${templar.x.toInt} storms=${pilot.storms} hydras=${foes.count(_.alive)}/4")
    assert(pilot.storms > 0, "loaded hull never cast under the retreat verdict")
    assert(foes.exists(! _.alive), "no hydra died to the stand")
    assert(templar.x > -200.0, "hull fled home instead of holding the line")
  }
}
