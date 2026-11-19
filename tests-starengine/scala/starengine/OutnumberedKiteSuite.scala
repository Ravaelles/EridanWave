package starengine

import Micro.Actions.Combat.Decisioning.Kiting.KiteAway
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Outnumbered kite discipline: fourteen dragoons against sixteen. The
 * line cannot win a stand-up trade down two guns - the only lever is
 * movement while cooling: yield ground, stretch the enemy line, rotate
 * the beating so no hull dies with a full gun. The shipped rule kites
 * while cooling past its cooldown gate once the wound ladder opens;
 * the plant stands and trades. Both seats (the engine lets foes shoot
 * first), kills decide. Tuning knob under test: the mild cooldown
 * gate - how much of each cycle the line spends yielding.
 */
class OutnumberedKiteSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Dragoon.name + StarEngineTags.Targeting.name + StarEngineTags.PvP.name) }

  private def foesOf(me: Body, world: World): Seq[Body] =
    if (world.ours.contains(me)) world.foes else world.ours

  /** Shipped rule: kite while cooling with an enemy in margin, else chase. */
  private class KitePilot(cooldownGate: Int) extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val foe = world.nearestFoeOf(me, foesOf(me, world), (_, _) => true)
      val inMargin = foe.exists(f => me.dist(f) <= me.rangeAgainst(f) + KiteAway.enemyMarginTiles * 32.0)
      if (foe.isDefined && me.cooldownLeft > cooldownGate && inMargin) {
        val f = foe.get
        val dx = me.x - f.x
        val dy = me.y - f.y
        val d = Math.sqrt(dx * dx + dy * dy)
        if (d > 0) MoveTo(me.x + dx / d * 128.0, me.y + dy / d * 128.0) else Hold
      } else foe.map(Chase).getOrElse(Hold)
    }
  }

  /** Plant: chase and swing, never yield ground. */
  private class PlantPilot extends Pilot {
    override def direct(me: Body, world: World): Directive =
      world.nearestFoeOf(me, foesOf(me, world), (_, _) => true).map(Chase).getOrElse(Hold)
  }

  /** Shared weakest: the line deletes one hull at a time. */
  private class FocusPilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val live = foesOf(me, world).filter(_.alive)
      live.sortBy(_.hp).headOption.map(Chase).getOrElse(Hold)
    }
  }

  /** Shipped shape with a shared pick: kite while cooling, all guns on the weakest. */
  private class FocusKitePilot(cooldownGate: Int) extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val live = foesOf(me, world).filter(_.alive)
      val pick = live.sortBy(_.hp).headOption
      val inMargin = pick.exists(f => me.dist(f) <= me.rangeAgainst(f) + KiteAway.enemyMarginTiles * 32.0)
      if (pick.isDefined && me.cooldownLeft > cooldownGate && inMargin) {
        val f = pick.get
        val dx = me.x - f.x
        val dy = me.y - f.y
        val d = Math.sqrt(dx * dx + dy * dy)
        if (d > 0) MoveTo(me.x + dx / d * 128.0, me.y + dy / d * 128.0) else Hold
      } else pick.map(Chase).getOrElse(Hold)
    }
  }

  private def grind(oursN: Int, foeN: Int, oursP: Body => Pilot, foeP: Body => Pilot): (Int, Int) = {
    val ours = (0 until oursN).map(i => new Body(Protoss.Dragoon, i * 40.0, 900.0, Protoss.Dragoon.maxTotalHealth))
    val foes = (0 until foeN).map(i => new Body(Protoss.Dragoon, i * 40.0, 100.0, Protoss.Dragoon.maxTotalHealth))
    val world = new World(ours, foes, (ours.map(o => o -> oursP(o)) ++ foes.map(f => f -> foeP(f))).toMap, Seq.empty)
    world.run(9000)
    (foeN - foes.count(_.alive), oursN - ours.count(_.alive))
  }

  test("plant mirrors trade hulls: the control") {
    val (aKills, bKills) = grind(14, 16, _ => new PlantPilot, _ => new PlantPilot)
    val (cKills, dKills) = grind(16, 14, _ => new PlantPilot, _ => new PlantPilot)
    println(s"DIAG control 14v16 dealt=$aKills taken=$bKills; 16v14 dealt=$cKills taken=$dKills")
    assert(aKills + bKills > 0, "mirrors must trade, not stall")
  }

  test("focus plus kite down two guns: the outnumbered answer") {
    // Status quo both seats: spread plant.
    val (sqA, sqB) = grind(14, 16, _ => new PlantPilot, _ => new PlantPilot)
    // Focus-plant and focus-kite as the 14-line, spread plant as the 16.
    val (fpKills, fpTaken) = grind(14, 16, _ => new FocusPilot, _ => new PlantPilot)
    val (fkKills, fkTaken) = grind(14, 16, _ => new FocusKitePilot(KiteAway.mildCooldown), _ => new PlantPilot)
    println(s"DIAG statusquo dealt=$sqA taken=$sqB; focus-plant dealt=$fpKills taken=$fpTaken; focus-kite dealt=$fkKills taken=$fkTaken")
    assert(fpKills + fkKills >= 2 * sqA, "focus must deal more than spread down two guns")
  }

  test("kiting moves hulls but wins no kills down two guns") {
    // Symmetric movement war, both seats: the 16-line wipes the 14-line
    // either way (numbers plus first shot decide). Kite concedes nothing
    // on deaths here, but it concedes volleys: the delete-race below is
    // where the outnumbered line actually gains.
    val (kiteKills, plantKillsA) = grind(14, 16, _ => new KitePilot(KiteAway.mildCooldown), _ => new PlantPilot)
    val (plantKillsB, kiteKillsB) = grind(14, 16, _ => new PlantPilot, _ => new KitePilot(KiteAway.mildCooldown))
    println(s"DIAG kite-seat dealt=$kiteKills taken=$plantKillsA; plant-seat dealt=$plantKillsB taken=$kiteKillsB")
    assert(kiteKills + kiteKillsB == plantKillsA + plantKillsB, "symmetric movement trades symmetric kills")
  }
}
