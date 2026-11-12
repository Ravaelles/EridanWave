package Tactic.Squads.defense

import Information.Geography.Chokes
import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.Pixel
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.UnitInfo
import Tactic.Squads.Squad
import Tactic.Squads.SquadAutomation
import Utilities.UnitFilters.IsAny

/**
 * The standing guard's post: enemies at any home base first, the natural
 * choke otherwise. One rule shared by Tactician (staffing) and the squad
 * (running), so the two can never disagree about where home is.
 */
object BaseGuardPost {

  def apply(): Pixel = threatened.getOrElse(choke)

  private def threatened: Option[Pixel] = With.geography.ourBases.filterNot(_.isMain)
    .filter(h => h.metro.units.exists(u => u.isEnemy && u.unitClass.isWarrior))
    .headOption.map(_.heart.center)

  private def choke: Pixel = Chokes.naturalChoke.map(_.pixelCenter)
    .getOrElse(With.geography.ourMain.heart.center)
}

/**
 * Pure base-guard engagement gate (no game access): an enemy this close, in
 * ground tiles, to a guarded base is fought - the guard never watches the
 * choke while the base dies. The caller measures along the ground, so
 * unreachable map corners never trip it.
 */
object BaseGuardFight {

  /** At or below this ground distance the guard engages, in tiles. */
  val engageRadiusTiles: Double = 30.0

  def shouldEngage(distTiles: Double): Boolean =
    distTiles <= engageRadiusTiles
}

/**
 * Standing home guard: posted from twenty warriors, scaled with the army,
 * answering enemies at any of our bases. Sized by Tactician through the
 * lock counter, so it shrinks through standard ablation when the army
 * bleeds. Dynamic defense squads are never staffed without a live division
 * and dissolve on their own once it ends; this is the only standing defense.
 */
class SquadBaseGuards extends Squad {

  // Standing guard, never a specialist: Dragoons and Zealots only.
  lock.setMatcher(IsAny(Protoss.Dragoon, Protoss.Zealot))

  override def launch(): Unit = {} // Staffed externally by Tactician

  // Latched by run() below: with an enemy on the doorstep the guard fights
  // now instead of waiting out the consensus latch at the choke.
  private var engagingBase: Boolean = false
  override def fightConsensus: Boolean = engagingBase || super.fightConsensus

  override def run(): Unit = {
    vicinity = BaseGuardPost()
    val nearest = nearestBaseEnemy
    engagingBase = nearest.isDefined
    setTargets(nearest.toVector ++ guardTargets)
    SquadAutomation.send(this)
  }

  // Nearest enemy by ground travel to any of our bases inside the guard
  // radius: the fight the guard exists for, even outside its metro.
  private def nearestBaseEnemy: Option[UnitInfo] = Maff.minBy(
    With.geography.ourBases.view.flatMap(base =>
      With.units.enemy.view
        .filter(e => e.likelyStillThere)
        .map(e => (e, base.heart.center.groundPixels(e.pixel) * Maff.inv32))
        .filter(_._2 <= BaseGuardFight.engageRadiusTiles)))(_._2).map(_._1)

  private def guardTargets: Vector[UnitInfo] = With.geography.ourBases
    .flatMap(_.metro.units)
    .filter(u => u.isEnemy && u.unitClass.isWarrior).toVector
}
