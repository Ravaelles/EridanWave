package starengine

import Micro.Actions.Protoss.ReaverShuttle.ReaverCoolWalk
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Cooling walk: one Reaver against two pressing Hydralisks. The old
 * brain stands its ground after every volley and gets chewed during the
 * minute-long cooldown - the live bill is StandShoot, a rally label,
 * StandShoot again, then a dead hull. The fixed brain walks off the
 * nearest gun while the shot is still nine frames out and holds only
 * to fire it, so the first hydra dies, the hull kites the cooldown out,
 * and the second hydra dies the same way. Speeds, reaches, damage and
 * cooldowns are the real UnitType numbers; both pilots call the shipped
 * cooling gate, the engine owns the loop only.
 */
class ReaverCoolWalkScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Reaver.name + StarEngineTags.Pressure.name) }

  private class KitePilot(useCoolWalk: Boolean) extends Pilot {
    var minFoeDist: Double = Double.PositiveInfinity

    override def direct(me: Body, world: World): Directive = {
      val foes = world.foes.filter(f => f.alive && ! f.kind.isBuilding)
      if (foes.isEmpty) return Hold
      val nearest = foes.minBy(me.dist)
      minFoeDist = Math.min(minFoeDist, me.dist(nearest))

      if (useCoolWalk && ReaverCoolWalk.shouldWalk(me.cooldownLeft, me.dist(nearest) / 32.0)) {
        val (dx, dy) = away(me, nearest.x, nearest.y)
        return MoveTo(me.x + dx * 128.0, me.y + dy * 128.0)
      }
      if (me.dist(nearest) <= me.rangeAgainst(nearest)) Hold
      else Chase(nearest)
    }

    private def away(me: Body, x: Double, y: Double): (Double, Double) = {
      val dx = me.x - x
      val dy = me.y - y
      val dist = Math.sqrt(dx * dx + dy * dy)
      if (dist > 0) (dx / dist, dy / dist) else (0.0, 1.0)
    }
  }

  private case class Outcome(reaverHp: Double, reaverAlive: Boolean, hydrasLeft: Int, minFoeDist: Double)

  private def runDuel(useCoolWalk: Boolean): Outcome = {
    val reaver = new Body(Protoss.Reaver, 0.0, 0.0, Protoss.Reaver.maxTotalHealth)
    val pack = Seq(
      new Body(Zerg.Hydralisk, -40.0, 300.0, Zerg.Hydralisk.maxTotalHealth),
      new Body(Zerg.Hydralisk, 40.0, 300.0, Zerg.Hydralisk.maxTotalHealth))
    val pilot = new KitePilot(useCoolWalk)
    val pilots: Map[Body, Pilot] = Map(reaver -> pilot) ++
      pack.map(h => h -> new Pilots.ZombieAttack(_.ours, (0.0, 0.0))).toMap
    val world = new World(Seq(reaver), pack, pilots, Seq.empty)
    var f = 0
    while (f < 1500 && reaver.alive && pack.exists(_.alive)) { world.step(); f += 1 }
    Outcome(reaver.hp, reaver.alive, pack.count(_.alive), pilot.minFoeDist)
  }

  test("a cooling reaver walks its cooldown out and kills both hydras") {
    val fixed = runDuel(useCoolWalk = true)
    println(s"DIAG fixed reaver=${fixed.reaverHp.toInt} hydrasLeft=${fixed.hydrasLeft} minFoe=${fixed.minFoeDist.toInt}px")
    assert(fixed.hydrasLeft == 0, "the kiting reaver never finished the second hydra")
    assert(fixed.reaverAlive, "the kiting reaver died anyway")
    assert(fixed.minFoeDist > 100.0, "the kiting reaver let the pack into melee")

    val old = runDuel(useCoolWalk = false)
    println(s"DIAG old reaver=${old.reaverHp.toInt} alive=${old.reaverAlive} hydrasLeft=${old.hydrasLeft} minFoe=${old.minFoeDist.toInt}px")
    assert(fixed.reaverHp > old.reaverHp, "the walk bought no hull over standing")
    assert(old.minFoeDist < 60.0, "the control never got swarmed - nothing to fix here")
  }
}
