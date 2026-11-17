package starengine

import ProxyBwapi.Races.Zerg
import org.scalatest.funsuite.AnyFunSuite

/**
 * Zombie targeting contract: nearest first, most wounded breaks ties,
 * nothing when nobody lives. Every scenario plays against this pick,
 * so the pick itself stays pinned here instead of drifting per suite.
 */
class ZombiePickSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Targeting.name) }

  private def ling(x: Double, y: Double, hpRatio: Double): Body = {
    val hp = (Zerg.Zergling.maxTotalHealth * hpRatio).toInt
    new Body(Zerg.Zergling, x, y, hp.toDouble)
  }

  test("nearest wins over most wounded") {
    val me = ling(0.0, 0.0, 1.0)
    val nearHealthy = ling(0.0, 100.0, 1.0)
    val farWounded = ling(0.0, 200.0, 0.1)
    assert(Pilots.ZombieAttack.pick(me, Seq(farWounded, nearHealthy)).contains(nearHealthy))
  }

  test("most wounded wins ties") {
    val me = ling(0.0, 0.0, 1.0)
    val healthy = ling(100.0, 0.0, 1.0)
    val wounded = ling(-100.0, 0.0, 0.2)
    assert(Pilots.ZombieAttack.pick(me, Seq(healthy, wounded)).contains(wounded))
  }

  test("dead bodies and empty lists pick nothing") {
    val me = ling(0.0, 0.0, 1.0)
    val corpse = ling(10.0, 0.0, 0.0)
    corpse.hp = 0.0
    assert(Pilots.ZombieAttack.pick(me, Seq(corpse)).isEmpty)
    assert(Pilots.ZombieAttack.pick(me, Seq.empty).isEmpty)
  }
}
