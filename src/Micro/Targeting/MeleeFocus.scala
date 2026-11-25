package Micro.Targeting

import Mathematics.Maff
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Melee shared chew (rule 19: squad assignment as a Target pick, not a
 * second scoring ladder): pack blades chew one target instead of each
 * dueling its own. Ranged focus needs volleys to line up; blades need
 * contact to line up - the weakest enemy two pack mates can both reach
 * dies first, then the pack re-picks together. Reavers keep their own
 * doctrine (scarabs, not blades); workers keep theirs.
 */
object MeleeFocus {

  /** A mate this far out still counts as reaching the shared pick, in tiles. */
  val packReachTiles: Double = 3.0

  final case class Candidate(swingsToKill: Int, distTiles: Double, id: Int)

  /** Swings the pack needs for the target: pack damage, never one hull's. */
  def swingsToKill(healthTotal: Double, packDamage: Double): Int =
    if (packDamage <= 0) Int.MaxValue else Math.max(1, Math.ceil(healthTotal / packDamage).toInt)

  /** Fewest swings first, then nearest to the pack, stable by id. */
  def choose(candidates: Iterable[Candidate]): Option[Candidate] =
    Maff.minBy(candidates)(c => (c.swingsToKill, c.distTiles, c.id))

  /**
   * The pack's shared chew for this blade, if the pack is on: weakest
   * enemy the pack can hit now that a pack mate can also reach.
   * None means "chew normally". Candidates come from the squad's
   * battle, not the hull's own targets - per-hull sets flap as sight
   * lines cross, while one squad-wide set gives every blade the same
   * answer by construction, so the pack converges instead of
   * fanning out.
   */
  def sharedTarget(attacker: FriendlyUnitInfo): Option[UnitInfo] = {
    val squad = attacker.squad.getOrElse(return None)
    if ( ! attacker.unitClass.canAttack || ! attacker.unitClass.melee) return None

    val pack = squad.attackers.view.filter(u =>
      u.alive && u.unitClass.melee && u.unitClass.canAttack && u.friendly.exists(_ != attacker)).toSeq
    if (pack.isEmpty) return None

    val candidates = squad.battleEnemies.filter(t =>
      t.alive && t.visible && ! t.unitClass.isBuilding && pack.exists(m => m.canAttack(t))).map(t => {
      val hitters = pack.filter(m => m.inRangeToAttack(t) || m.pixelDistanceEdge(t) <= packReachTiles * 32.0)
      (t, hitters)
    }).filter(_._2.size >= 1).map { case (t, hitters) =>
      val damage = (attacker +: hitters).map(_.damageOnNextHitAgainst(t)).sum.toDouble
      (t, Candidate(swingsToKill(t.totalHealth, damage), t.pixelDistanceCenter(squad.centroidGround) * Maff.inv32, t.id))
    }
    choose(candidates.map(_._2)).flatMap(chosen =>
      candidates.find(_._2.id == chosen.id).map(_._1))
  }
}
