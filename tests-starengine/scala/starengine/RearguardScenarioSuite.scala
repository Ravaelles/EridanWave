package starengine

import ProxyBwapi.Races.{Protoss, Terran}
import Tactic.Squads.Decisions.{ClusterPosture, ClusterRoles}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Rearguard cover, step by step: four Dragoons break off from two
 * Marines, and the shipped role table names the covering hulls. Pilots
 * call the shipped ClusterRoles.assign on the same facts production
 * reads (fast, ranged, healthy, distance) and the rearguard holds its
 * shot while the main body walks home - instead of the whole line
 * standing to die or the cover running first. Speeds, reaches and
 * damage are the real UnitType numbers.
 */
class RearguardScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Roles.name + StarEngineTags.Retreat.name) }

  /** Pilot, production order: the shipped table assigns, legs execute. */
  private class RearguardPilot(allies: Seq[Body], foe: Body) extends Pilot {
    var coveredShots = 0
    var fled = 0

    override def direct(me: Body, world: World): Directive = {
      val meanSpeed = allies.map(_.kind.topSpeed).sum / allies.size
      val seekers = allies.map(a => ClusterRoles.Seeker(
        fast = a.kind.topSpeed >= meanSpeed,
        ranged = ! a.kind.melee,
        healthy = a.hp >= a.kind.maxTotalHealth / 2,
        distToEnemy = a.dist(foe)))
      val role = ClusterRoles.assign(ClusterPosture.Withdraw, seekers.toVector)(
        allies.indexOf(me))
      role match {
        case ClusterRoles.Rearguard =>
          coveredShots += 1
          if (me.dist(foe) <= me.rangeAgainst(foe)) Hold else Chase(foe)
        case _ =>
          fled += 1
          val dx = me.x - foe.x
          val dy = me.y - foe.y
          val dist = Math.sqrt(dx * dx + dy * dy)
          if (dist > 0) MoveTo(me.x + dx / dist * 200.0, me.y + dy / dist * 200.0)
          else Hold
      }
    }
  }

  test("the rearguard covers while the main body walks home") {
    val ours = (0 until 4).map(i =>
      new Body(Protoss.Dragoon, -i * 60.0, 500.0, Protoss.Dragoon.maxTotalHealth)).toSeq
    val marine = new Body(Terran.Marine, 400.0, 500.0, Terran.Marine.maxTotalHealth)
    val pilots = ours.map(o => o -> new RearguardPilot(ours, marine)).toMap
    val world = new World(ours, Seq(marine),
      pilots ++ Map(marine -> new Pilots.Hold), Seq.empty)

    var f = 0
    while (f < 600 && ours.exists(_.alive) && marine.alive) {
      world.step()
      f = world.frame
    }
    val covered = pilots.values.map(_.coveredShots).sum
    val fled = pilots.values.map(_.fled).sum
    println(s"DIAG rearguard covered=$covered fled=$fled marines=${marine.hp}")
    assert(covered > 0, "no hull covered the withdrawal")
    assert(fled > covered, "the main body did not walk home")
    assert(marine.hp < Terran.Marine.maxTotalHealth, "cover never shot")
  }
}
