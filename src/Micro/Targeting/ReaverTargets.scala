package Micro.Targeting

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.Races.{Protoss, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Reaver special picks (thin seam over a pure pick): a pre-`Target`
 * doctrine, not a second ladder (rule 15) - the pick lands before
 * `Target.choose` because masonry must never outrank the guns that
 * outrange the duel. Vs Zerg the nearest detected Lurker is the fight
 * (spines outrange everything the Reaver would rather chew); vs
 * Protoss it is the nearest Dark Templar. Fires only with scarabs to
 * spend and a hull on its own feet - an empty or embarked hull keeps
 * the old targeting untouched.
 */
object ReaverTargets {

  /** One candidate: how far, and whether the guns can see it. */
  final case class Mark(distTiles: Double, detected: Boolean)

  /** Nearest detected killer; nothing when nobody is seen. */
  def pickNearestDetected(marks: Seq[Mark]): Option[Int] =
    Maff.minBy(marks.view.filter(_.detected))(_.distTiles).map(marks.indexOf(_))

  def nearestLurker(unit: FriendlyUnitInfo): Option[UnitInfo] =
    Maff.minBy(With.units.enemy.view.filter(e =>
      e.alive && Zerg.Lurker(e) && e.visible))(
      unit.pixelDistanceEdge(_))

  def nearestTemplar(unit: FriendlyUnitInfo): Option[UnitInfo] =
    Maff.minBy(With.units.enemy.view.filter(e =>
      e.alive && Protoss.DarkTemplar(e) && e.visible))(
      unit.pixelDistanceEdge(_))

  /**
   * Doctrine override (game-coupled seam, pure pick inside): replaces a
   * masonry duel with the killer when one shows, otherwise leaves the
   * duel alone. Player records stay empty on UMS, so the race reads off
   * seen units instead of the lobby.
   */
  def check(unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Reaver(unit)) return false
    if (unit.loaded || unit.scarabs <= 0) return false

    val foes =
      if (With.units.enemy.exists(e => e.alive && e.unitClass.isZerg))
        With.units.enemy.view.filter(e => e.alive && Zerg.Lurker(e)).toSeq
      else if (With.units.enemy.exists(e => e.alive && e.unitClass.isProtoss))
        With.units.enemy.view.filter(e => e.alive && Protoss.DarkTemplar(e)).toSeq
      else Seq.empty
    if (foes.isEmpty) return false

    val pick = pickNearestDetected(foes.map(e =>
      Mark(unit.pixelDistanceEdge(e) * Maff.inv32, e.visible)))
      .map(foes(_))
    if (pick.isEmpty) return false

    unit.agent.toAttack = pick
    unit.agent.act("ReaverTargets")
    true
  }
}
