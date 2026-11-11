package Tactic.Tactics.Scouting.Monitor

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

  def lurkerThreatensMain(distTiles: Double): Boolean =
    distTiles < lurkerNearMainTiles

  def siteBlocked(distTiles: Double): Boolean =
    distTiles < blockerTiles

  /** Planned base with enemy presence, if any. */
  def siteToReveal(): Option[Base] =
    With.geography.bases.find(b =>
      b.plannedExpoRecently && With.units.enemy.exists(e =>
        ! e.flying && siteBlocked(e.tileDistanceCenter(b.heart.center))))

  /** The first eye may go even when it's the only one. */
  def mayUseFirstEye: Boolean =
    ! With.units.enemy.exists(e => Zerg.Lurker(e) &&
      lurkerThreatensMain(e.tileDistanceCenter(With.geography.ourMain.heart.center)))
}
