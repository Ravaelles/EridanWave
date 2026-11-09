package Tactic.Tactics.Scouting

import Information.Geography.Types.Base
import Lifecycle.With
import ProxyBwapi.Races.Zerg

/**
 * Expo reveal duty (Atlantis DetectNewBasePotentiallyBlocked): which planned
 * base needs an eye, and whether the first eye may go. Stateless decision;
 * TacticMonitor owns the actual assignment (single lock) and the death
 * memory, so the priority lives in exactly one method.
 */
object RevealExpansionBlockedWithHiddenUnit {

  /** Lurkers this close to our main keep the first eye home. */
  val lurkerNearMainTiles: Double = 50.0

  /** Enemies this close to the site count as blocking presence. */
  val blockerTiles: Double = 4.0

  /** Planned base with enemy presence, if any. */
  def siteToReveal(): Option[Base] =
    With.geography.bases.find(b =>
      b.plannedExpoRecently && With.units.enemy.exists(e =>
        ! e.flying && e.pixelDistanceCenter(b.heart.center) < blockerTiles * 32))

  /** The first eye may go even when it's the only one. */
  def mayUseFirstEye: Boolean =
    ! With.units.enemy.exists(e => Zerg.Lurker(e) &&
      e.pixelDistanceCenter(With.geography.ourMain.heart.center) < lurkerNearMainTiles * 32)
}
