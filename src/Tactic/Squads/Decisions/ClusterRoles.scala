package Tactic.Squads.Decisions

import Tactic.Squads.Decisions.ClusterPosture._

/**
 * Cluster roles (no game access): within the shared posture each member
 * gets one job, so a flanker seeing different threats still serves the
 * cluster verdict instead of voting against it. Roles only modulate how -
 * spearheads close first, flanks circle, supports stand off, the rearguard
 * covers a withdrawal. Small armies never detach a rearguard: below twenty
 * warriors the cluster withdraws whole (ClusterSplit.keepTogether).
 */
object ClusterRoles {

  sealed trait Role

  /** Closest to the enemy: closes first, takes contact. */
  case object Spearhead extends Role

  /** Fast and off the spearhead line: circles instead of charging. */
  case object Flank extends Role

  /** Slow or long-ranged: stands off at its own reach. */
  case object Support extends Role

  /** Withdrawal cover: fast and healthy, last out. */
  case object Rearguard extends Role

  /** The line itself: holds, marches, shoots. */
  case object MainBody extends Role

  /** Plain member facts, caller-side order: result aligns by index. */
  final case class Seeker(fast: Boolean, ranged: Boolean, healthy: Boolean, distToEnemy: Double)

  /** Spearheads at most this many members, never more than a third. */
  val maxSpearheads: Int = 3

  def assign(posture: Posture, seekers: Vector[Seeker]): Vector[Role] = {
    if (seekers.isEmpty) return Vector.empty

    posture match {
      case Withdraw => assignWithdraw(seekers)
      case Fight    => assignFight(seekers)
      case _        => seekers.map(_ => MainBody)
    }
  }

  private def assignWithdraw(seekers: Vector[Seeker]): Vector[Role] = {
    val roles = Array.fill[Role](seekers.size)(MainBody: Role)
    val cover = seekers.indices.filter(i => seekers(i).fast && seekers(i).healthy)
    val rearCount = Math.min(cover.size, Math.max(1, seekers.size / 3))
    cover.take(rearCount).foreach(i => roles(i) = Rearguard)
    roles.toVector
  }

  private def assignFight(seekers: Vector[Seeker]): Vector[Role] = {
    val roles = Array.fill[Role](seekers.size)(MainBody: Role)
    val byDistance = seekers.indices.sortBy(seekers(_).distToEnemy)
    val heads = byDistance.take(Math.min(maxSpearheads, Math.max(1, seekers.size / 3)))
    heads.foreach(i => roles(i) = Spearhead)
    seekers.indices.foreach(i => {
      if (roles(i) == MainBody) {
        if (seekers(i).fast) roles(i) = Flank
        else if (seekers(i).ranged) roles(i) = Support
      }
    })
    roles.toVector
  }
}
