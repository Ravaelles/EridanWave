package starengine

import Micro.Actions.Combat.Decisioning.Kiting.KiteAway
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Mirror kite discipline: three zealots against three. Kiting a mirror
 * never opens ground (equal speed), it only donates free hits while the
 * gun cools and splits the pack that should brawl together. The shipped
 * melee kite answers when cooling with melee nearby; the plant holds its
 * ground and swings instead. Both seats, kills decide.
 */
class MirrorKiteSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Zealot.name + StarEngineTags.Targeting.name + StarEngineTags.PvP.name) }

  /** Shipped rule: kite while cooling with melee nearby, else chase. */
  private class KitePilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val foe = world.nearestFoeOf(me, world.foes, (_, _) => true)
      val inMargin = foe.exists(f => me.dist(f) <= me.rangeAgainst(f) + KiteAway.enemyMarginTiles * 32.0)
      val meleeNear = foe.exists(f => f.kind == Protoss.Zealot && me.dist(f) <= me.rangeAgainst(f) + KiteAway.enemyMarginTiles * 32.0)
      val wound = (me.maxHp - me.hp).toInt
      if (foe.isDefined && KiteAway.shouldRetreat(me.hp.toInt, me.lossFraction, me.cooldownLeft, inMargin, meleeNear, wound, 0, me.hp.toInt)) {
        val f = foe.get
        val dx = me.x - f.x
        val dy = me.y - f.y
        val d = Math.sqrt(dx * dx + dy * dy)
        if (d > 0) MoveTo(me.x + dx / d * 128.0, me.y + dy / d * 128.0) else Hold
      } else foe.map(Chase).getOrElse(Hold)
    }
  }

  /** Plant: chase and swing, never yield ground to a mirror. */
  private class PlantPilot extends Pilot {
    override def direct(me: Body, world: World): Directive =
      world.nearestFoeOf(me, world.foes, (_, _) => true).map(Chase).getOrElse(Hold)
  }

  private def grind(oursP: Body => Pilot, foeP: Body => Pilot): (Int, Int) = {
    val ours = (-1 to 1).map(i => new Body(Protoss.Zealot, i * 40.0, 900.0, Protoss.Zealot.maxTotalHealth))
    val foes = (-1 to 1).map(i => new Body(Protoss.Zealot, i * 40.0, 100.0, Protoss.Zealot.maxTotalHealth))
    val world = new World(ours, foes, (ours.map(o => o -> oursP(o)) ++ foes.map(f => f -> foeP(f))).toMap, Seq.empty)
    world.run(6000)
    (3 - foes.count(_.alive), 3 - ours.count(_.alive))
  }

  test("planting beats kiting the mirror, both seats") {
    val (kiteKills, plantKillsA) = grind(_ => new KitePilot, _ => new PlantPilot)
    val (plantKillsB, kiteKillsB) = grind(_ => new PlantPilot, _ => new KitePilot)
    println(s"DIAG kite-seat kiteKills=$kiteKills plantKills=$plantKillsA; plant-seat plantKills=$plantKillsB kiteKills=$kiteKillsB")
    assert(plantKillsA + plantKillsB >= kiteKills + kiteKillsB, "planting must not trade down to kiting in a mirror")
  }
}
