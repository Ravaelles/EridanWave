package starengine

import Micro.Actions.Combat.Decisioning.Kiting.KiteAway
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Wounded-duel discipline: one dragoon at 60 total HP against a fresh
 * zealot - the 1DragWounded map. Standing trades volleys the hull no
 * longer has; kiting while the gun cools spends waiting time on
 * distance instead of damage. The shipped rule kites below 61 HP when
 * cooling with the melee chaser in margin; the plant stands and trades.
 * Crossed seats (the engine lets foes shoot first, so a win in one
 * seat proves nothing): kills plus survivals decide.
 */
class WoundedDuelSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Dragoon.name + StarEngineTags.Zealot.name + StarEngineTags.Targeting.name + StarEngineTags.PvP.name) }

  private def foesOf(me: Body, world: World): Seq[Body] =
    if (world.ours.contains(me)) world.foes else world.ours

  /** Shipped rule, verbatim: kite while cooling under the shipped gate, else chase. */
  private class KitePilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val foe = world.nearestFoeOf(me, foesOf(me, world), (_, _) => true)
      val inMargin = foe.exists(f => me.dist(f) <= me.rangeAgainst(f) + KiteAway.enemyMarginTiles * 32.0)
      val meleeNear = foe.exists(f => f.kind == Protoss.Zealot && me.dist(f) <= me.rangeAgainst(f) + KiteAway.enemyMarginTiles * 32.0)
      if (foe.isDefined && KiteAway.shouldRetreat(me.hp.toInt, me.cooldownLeft, inMargin, meleeNear)) {
        val f = foe.get
        val dx = me.x - f.x
        val dy = me.y - f.y
        val d = Math.sqrt(dx * dx + dy * dy)
        if (d > 0) MoveTo(me.x + dx / d * 128.0, me.y + dy / d * 128.0) else Hold
      } else foe.map(Chase).getOrElse(Hold)
    }
  }

  /** Plant: chase and swing, never yield ground, cooling or not. */
  private class PlantPilot extends Pilot {
    override def direct(me: Body, world: World): Directive =
      world.nearestFoeOf(me, foesOf(me, world), (_, _) => true).map(Chase).getOrElse(Hold)
  }

  /** One duel, dragoon doctrine under test, zealot plants. Returns (dragoon alive, zealot dead, dragoon death frame or Int.MaxValue). */
  private def duel(dragoonP: Body => Pilot, dragoonIsOurs: Boolean): (Boolean, Boolean, Int) = {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 900.0, 60.0)
    val zealot = new Body(Protoss.Zealot, 0.0, 100.0, Protoss.Zealot.maxTotalHealth)
    val (ours, foes) = if (dragoonIsOurs) (Seq(dragoon), Seq(zealot)) else (Seq(zealot), Seq(dragoon))
    val world = new World(ours, foes, Map(dragoon -> dragoonP(dragoon), zealot -> new PlantPilot), Seq.empty)
    var remaining = 9000
    var deathAt = Int.MaxValue
    while (remaining > 0 && dragoon.alive && zealot.alive) {
      world.step()
      remaining -= 1
      if ( ! dragoon.alive && deathAt == Int.MaxValue) deathAt = world.frame
    }
    (dragoon.alive, ! zealot.alive, deathAt)
  }

  test("chase math: print the raw speeds and ranges once") {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 0.0, Protoss.Dragoon.maxTotalHealth)
    val zealot = new Body(Protoss.Zealot, 100.0, 0.0, Protoss.Zealot.maxTotalHealth)
    println(s"DIAG dragoon speed=${dragoon.speed} range=${dragoon.rangeAgainst(zealot)} cd=${dragoon.cooldownAgainst(zealot)} dmg=${dragoon.damageAgainst(zealot)} hp=${dragoon.maxHp}")
    println(s"DIAG zealot speed=${zealot.speed} range=${zealot.rangeAgainst(dragoon)} cd=${zealot.cooldownAgainst(dragoon)} dmg=${zealot.damageAgainst(dragoon)} hp=${zealot.maxHp}")
    assert(dragoon.speed > 0 && zealot.speed > 0)
  }

  test("a wounded dragoon kites a fresh zealot, planting does not") {
    val (kiteAliveA, kiteKillsA, kiteDeathA) = duel(_ => new KitePilot, dragoonIsOurs = true)
    val (kiteAliveB, kiteKillsB, kiteDeathB) = duel(_ => new KitePilot, dragoonIsOurs = false)
    val (plantAliveA, plantKillsA, plantDeathA) = duel(_ => new PlantPilot, dragoonIsOurs = true)
    val (plantAliveB, plantKillsB, plantDeathB) = duel(_ => new PlantPilot, dragoonIsOurs = false)
    val kiteScore = Seq(kiteAliveA, kiteKillsA, kiteAliveB, kiteKillsB).count(identity)
    val plantScore = Seq(plantAliveA, plantKillsA, plantAliveB, plantKillsB).count(identity)
    println(s"DIAG kite alive=$kiteAliveA,$kiteAliveB deadAt=$kiteDeathA,$kiteDeathB kills=$kiteKillsA,$kiteKillsB; plant alive=$plantAliveA,$plantKillsB deadAt=$plantDeathA,$plantDeathB kills=$plantKillsA,$plantKillsB")
    assert(kiteScore >= plantScore, "kiting must not trade down to planting a wounded duel")
    assert(kiteDeathA + kiteDeathB >= plantDeathA + plantDeathB, "kiting must buy time over planting")
  }
}
