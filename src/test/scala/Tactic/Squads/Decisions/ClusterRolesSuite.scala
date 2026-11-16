package Tactic.Squads.Decisions

import Tactic.Squads.Decisions.ClusterPosture.{Fight, HoldLine, Regroup, Withdraw}
import Tactic.Squads.Decisions.ClusterRoles._
import org.scalatest.funsuite.AnyFunSuite

class ClusterRolesSuite extends AnyFunSuite {

  private def seeker(
    fast: Boolean = false,
    ranged: Boolean = false,
    healthy: Boolean = true,
    dist: Double = 10.0): Seeker =
    Seeker(fast, ranged, healthy, dist)

  test("empty cluster assigns nothing") {
    assert(ClusterRoles.assign(Fight, Vector.empty).isEmpty)
  }

  test("hold and regroup keep everybody in the line") {
    val seekers = Vector(seeker(fast = true, dist = 3.0), seeker(ranged = true), seeker())
    assert(ClusterRoles.assign(HoldLine, seekers).forall(_ == MainBody))
    assert(ClusterRoles.assign(Regroup, seekers).forall(_ == MainBody))
  }

  test("the closest close, the fast circle, the ranged stand off") {
    val seekers = Vector(
      seeker(dist = 2.0),
      seeker(dist = 5.0),
      seeker(fast = true, dist = 6.0),
      seeker(ranged = true, dist = 7.0),
      seeker(dist = 8.0),
      seeker(dist = 9.0))
    val roles = ClusterRoles.assign(Fight, seekers)
    assert(roles(0) == Spearhead)
    assert(roles(1) == Spearhead)
    assert(roles(2) == Flank)
    assert(roles(3) == Support)
    assert(roles(4) == MainBody)
    assert(roles(5) == MainBody)
  }

  test("a lone fighter still takes contact") {
    assert(ClusterRoles.assign(Fight, Vector(seeker())) == Vector(Spearhead))
  }

  test("withdrawal covers with the fast and healthy, capped at a third") {
    val seekers = Vector(
      seeker(fast = true, healthy = true),
      seeker(fast = true, healthy = false),
      seeker(),
      seeker(fast = true, healthy = true),
      seeker(),
      seeker())
    val roles = ClusterRoles.assign(Withdraw, seekers)
    assert(roles.count(_ == Rearguard) == 2)
    assert(roles(0) == Rearguard)
    assert(roles(3) == Rearguard)
    assert(roles(1) == MainBody)
  }

  test("no cover when nobody can run") {
    val seekers = Vector(seeker(), seeker(), seeker())
    assert(ClusterRoles.assign(Withdraw, seekers).forall(_ == MainBody))
  }
}
