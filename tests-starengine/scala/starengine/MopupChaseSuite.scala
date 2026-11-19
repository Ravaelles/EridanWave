package starengine

import Micro.Actions.Combat.Decisioning.Kiting.KiteAway
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Mop-up chase: a kiting Hydralisk against a closing Dragoon. The foe
 * flees while the chaser is far, turns a volley when closed on, and
 * flees while cooling - the LingsHydra endgame in miniature. The
 * question is the closer's: run through the cooldown and keep
 * closing, or kite on it and yield the ground just gained. Both
 * pilots chase when out of reach; they differ only in range.
 */
class MopupChaseSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Dragoon.name + StarEngineTags.Targeting.name + StarEngineTags.PvZ.name) }

  private def foesOf(me: Body, world: World): Seq[Body] =
    if (world.ours.contains(me)) world.foes else world.ours

  /** Kiting foe: flee far away, turn a volley in range, flee while cooling. */
  private class KiterPilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val foe = world.nearestFoeOf(me, foesOf(me, world), (_, _) => true)
      foe match {
        case None => Hold
        case Some(f) =>
          val d = me.dist(f)
          if (d <= me.rangeAgainst(f) && me.cooldownLeft == 0) Chase(f)
          else if (d <= me.rangeAgainst(f)) {
            val dx = me.x - f.x
            val dy = me.y - f.y
            val n = Math.sqrt(dx * dx + dy * dy)
            if (n > 0) MoveTo(me.x + dx / n * 128.0, me.y + dy / n * 128.0) else Hold
          } else Chase(f)
      }
    }
  }

  /** Closer that never kites: run at the foe, shoot when ready. */
  private class RunThroughPilot extends Pilot {
    override def direct(me: Body, world: World): Directive =
      world.nearestFoeOf(me, foesOf(me, world), (_, _) => true).map(Chase).getOrElse(Hold)
  }

  /** Closer that kites on cooldown per the shipped gate. */
  private class KiteCloserPilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val foe = world.nearestFoeOf(me, foesOf(me, world), (_, _) => true)
      val inMargin = foe.exists(f => me.dist(f) <= me.rangeAgainst(f) + KiteAway.enemyMarginTiles * 32.0)
      val meleeNear = false
      if (foe.isDefined && KiteAway.shouldRetreat(me.hp.toInt, me.cooldownLeft, inMargin, meleeNear)) {
        val f = foe.get
        val dx = me.x - f.x
        val dy = me.y - f.y
        val d = Math.sqrt(dx * dx + dy * dy)
        if (d > 0) MoveTo(me.x + dx / d * 128.0, me.y + dy / d * 128.0) else Hold
      } else foe.map(Chase).getOrElse(Hold)
    }
  }

  private def chase(closer: Body => Pilot): (Double, Double, Int) = {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 900.0, Protoss.Dragoon.maxTotalHealth)
    val hydra = new Body(Zerg.Hydralisk, 0.0, 100.0, Zerg.Hydralisk.maxTotalHealth)
    val world = new World(Seq(dragoon), Seq(hydra), Map(dragoon -> closer(dragoon), hydra -> new KiterPilot), Seq.empty)
    world.run(9000)
    (hydra.hp, dragoon.hp, world.frame)
  }

  test("chase math: print the raw speeds and ranges once") {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 0.0, Protoss.Dragoon.maxTotalHealth)
    val hydra = new Body(Zerg.Hydralisk, 100.0, 0.0, Zerg.Hydralisk.maxTotalHealth)
    println(s"DIAG dragoon speed=${dragoon.speed} range=${dragoon.rangeAgainst(hydra)} cd=${dragoon.cooldownAgainst(hydra)} dmg=${dragoon.damageAgainst(hydra)} hp=${dragoon.maxHp}")
    println(s"DIAG hydra speed=${hydra.speed} range=${hydra.rangeAgainst(dragoon)} cd=${hydra.cooldownAgainst(dragoon)} dmg=${hydra.damageAgainst(dragoon)} hp=${hydra.maxHp}")
    assert(dragoon.speed > 0 && hydra.speed > 0)
  }

  test("the closer runs through the cooldown, it does not kite away") {
    val (runFoeHp, runOwnHp, _) = chase(_ => new RunThroughPilot)
    val (kiteFoeHp, kiteOwnHp, _) = chase(_ => new KiteCloserPilot)
    println(f"DIAG runThrough foeHp=$runFoeHp%.0f ownHp=$runOwnHp%.0f; kiteCloser foeHp=$kiteFoeHp%.0f ownHp=$kiteOwnHp%.0f")
    assert(runFoeHp <= kiteFoeHp, "yielding ground while closing donates the kill")
  }
}
